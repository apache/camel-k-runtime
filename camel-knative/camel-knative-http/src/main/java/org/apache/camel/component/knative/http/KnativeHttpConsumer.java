/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.knative.http;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Message;
import org.apache.camel.NoTypeConversionAvailableException;
import org.apache.camel.Processor;
import org.apache.camel.TypeConverter;
import org.apache.camel.component.knative.spi.KnativeEnvironment;
import org.apache.camel.spi.HeaderFilterStrategy;
import org.apache.camel.support.DefaultConsumer;
import org.apache.camel.support.DefaultMessage;
import org.apache.camel.support.ExchangeHelper;
import org.apache.camel.support.MessageHelper;

public class KnativeHttpConsumer extends DefaultConsumer implements KnativeHttp.PredicatedHandler {
    private final KnativeHttpTransport transport;
    private final Predicate<HttpServerRequest> filter;
    private final KnativeHttp.ServerKey key;
    private final KnativeEnvironment.KnativeServiceDefinition serviceDefinition;
    private final HeaderFilterStrategy headerFilterStrategy;

    public KnativeHttpConsumer(
            KnativeHttpTransport transport,
            Endpoint endpoint,
            KnativeEnvironment.KnativeServiceDefinition serviceDefinition,
            Processor processor) {

        super(endpoint, processor);

        this.transport = transport;
        this.serviceDefinition = serviceDefinition;
        this.headerFilterStrategy = new KnativeHttpHeaderFilterStrategy();
        this.key = new KnativeHttp.ServerKey(serviceDefinition.getHost(), serviceDefinition.getPortOrDefault(KnativeHttp.DEFAULT_PORT));
        this.filter = KnativeHttpSupport.createFilter(serviceDefinition);
    }

    @Override
    protected void doStart() throws Exception {
        this.transport.getDispatcher(key).bind(this);

        super.doStart();
    }

    @Override
    protected void doStop() throws Exception {
        this.transport.getDispatcher(key).unbind(this);

        super.doStop();
    }

    @Override
    public boolean canHandle(HttpServerRequest request) {
        return filter.test(request);
    }

    @Override
    public void handle(HttpServerRequest request) {
        if (request.method() == HttpMethod.POST) {
            final Exchange exchange = getEndpoint().createExchange(ExchangePattern.InOut);
            final Message in = toMessage(request, exchange);

            request.bodyHandler(buffer -> {
                in.setBody(buffer.getBytes());

                exchange.setIn(in);

                try {
                    createUoW(exchange);
                    getAsyncProcessor().process(exchange, doneSync -> {
                        try {
                            HttpServerResponse response = toHttpResponse(request, exchange.getMessage());
                            Buffer body = null;

                            if (request.response().getStatusCode() != 204) {
                                body = computeResponseBody(exchange.getMessage());

                                // set the content type in the response.
                                String contentType = MessageHelper.getContentType(exchange.getMessage());
                                if (contentType != null) {
                                    // set content-type
                                    response.putHeader(Exchange.CONTENT_TYPE, contentType);
                                }
                            }

                            if (body != null) {
                                request.response().end(body);
                            } else {
                                request.response().setStatusCode(204);
                                request.response().end();
                            }
                        } catch (Exception e) {
                            getExceptionHandler().handleException(e);
                        }
                    });
                } catch (Exception e) {
                    getExceptionHandler().handleException(e);

                    request.response().setStatusCode(500);
                    request.response().putHeader(Exchange.CONTENT_TYPE, "text/plain");
                    request.response().end(e.getMessage());
                } finally {
                    doneUoW(exchange);
                }
            });
        } else {
            request.response().setStatusCode(405);
            request.response().putHeader(Exchange.CONTENT_TYPE, "text/plain");
            request.response().end("Unsupported method");

            throw new IllegalArgumentException("Unsupported method: " + request.method());
        }
    }

    private Message toMessage(HttpServerRequest request, Exchange exchange) {
        Message message = new DefaultMessage(exchange.getContext());
        String path = request.path();

        if (serviceDefinition.getPath() != null) {
            String endpointPath = serviceDefinition.getPath();
            String matchPath = path.toLowerCase(Locale.US);
            String match = endpointPath.toLowerCase(Locale.US);

            if (matchPath.startsWith(match)) {
                path = path.substring(endpointPath.length());
            }
        }

        for (Map.Entry<String, String> entry : request.headers().entries()) {
            if (!headerFilterStrategy.applyFilterToExternalHeaders(entry.getKey(), entry.getValue(), exchange)) {
                KnativeHttpSupport.appendHeader(message.getHeaders(), entry.getKey(), entry.getValue());
            }
        }
        for (Map.Entry<String, String> entry : request.params().entries()) {
            if (!headerFilterStrategy.applyFilterToExternalHeaders(entry.getKey(), entry.getValue(), exchange)) {
                KnativeHttpSupport.appendHeader(message.getHeaders(), entry.getKey(), entry.getValue());
            }
        }

        message.setHeader(Exchange.HTTP_PATH, path);
        message.setHeader(Exchange.HTTP_METHOD, request.method());
        message.setHeader(Exchange.HTTP_URI, request.uri());
        message.setHeader(Exchange.HTTP_QUERY, request.query());

        return message;
    }

    private HttpServerResponse toHttpResponse(HttpServerRequest request, Message message) {
        final HttpServerResponse response = request.response();
        final boolean failed = message.getExchange().isFailed();
        final int defaultCode = failed ? 500 : 200;
        final int code = message.getHeader(Exchange.HTTP_RESPONSE_CODE, defaultCode, int.class);
        final TypeConverter tc = message.getExchange().getContext().getTypeConverter();

        response.setStatusCode(code);

        for (Map.Entry<String, Object> entry : message.getHeaders().entrySet()) {
            final String key = entry.getKey();
            final Object value = entry.getValue();

            for (Object it: org.apache.camel.support.ObjectHelper.createIterable(value, null)) {
                String headerValue = tc.convertTo(String.class, it);
                if (headerValue == null) {
                    continue;
                }
                if (!headerFilterStrategy.applyFilterToCamelHeaders(key, headerValue, message.getExchange())) {
                    response.putHeader(key, headerValue);
                }
            }
        }

        return response;
    }

    private Buffer computeResponseBody(Message message) throws NoTypeConversionAvailableException {
        Object body = message.getBody();
        Exception exception = message.getExchange().getException();

        if (exception != null) {
            // we failed due an exception so print it as plain text
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            exception.printStackTrace(pw);

            // the body should then be the stacktrace
            body = sw.toString().getBytes(StandardCharsets.UTF_8);
            // force content type to be text/plain as that is what the stacktrace is
            message.setHeader(Exchange.CONTENT_TYPE, "text/plain");

            // and mark the exception as failure handled, as we handled it by returning
            // it as the response
            ExchangeHelper.setFailureHandled(message.getExchange());
        }

        return body != null
            ? Buffer.buffer(message.getExchange().getContext().getTypeConverter().mandatoryConvertTo(byte[].class, body))
            : null;
    }

}

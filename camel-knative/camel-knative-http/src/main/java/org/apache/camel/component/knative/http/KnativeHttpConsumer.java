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
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.NoTypeConversionAvailableException;
import org.apache.camel.Processor;
import org.apache.camel.TypeConverter;
import org.apache.camel.component.knative.spi.KnativeEnvironment;
import org.apache.camel.component.knative.spi.KnativeTransportConfiguration;
import org.apache.camel.component.platform.http.vertx.VertxPlatformHttpRouter;
import org.apache.camel.spi.HeaderFilterStrategy;
import org.apache.camel.support.DefaultConsumer;
import org.apache.camel.support.ExchangeHelper;
import org.apache.camel.support.MessageHelper;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KnativeHttpConsumer extends DefaultConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(KnativeHttpConsumer.class);

    private final KnativeTransportConfiguration configuration;
    private final Predicate<HttpServerRequest> filter;
    private final KnativeEnvironment.KnativeServiceDefinition serviceDefinition;
    private final VertxPlatformHttpRouter router;
    private final HeaderFilterStrategy headerFilterStrategy;

    private String basePath;
    private Route route;

    public KnativeHttpConsumer(
        KnativeTransportConfiguration configuration,
        Endpoint endpoint,
        KnativeEnvironment.KnativeServiceDefinition serviceDefinition,
        VertxPlatformHttpRouter router,
        Processor processor) {

        super(endpoint, processor);

        this.configuration = configuration;
        this.serviceDefinition = serviceDefinition;
        this.router = router;
        this.headerFilterStrategy = new KnativeHttpHeaderFilterStrategy();
        this.filter = KnativeHttpSupport.createFilter(serviceDefinition);
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    @Override
    protected void doStart() throws Exception {
        if (!ObjectHelper.isEmpty(serviceDefinition.getHost())) {
            LOGGER.warn("Unsupported property `host` on definition={}", serviceDefinition.getName());
        }
        if (serviceDefinition.getPort() >= 0) {
            LOGGER.warn("Unsupported property `port` on definition={}", serviceDefinition.getName());
        }

        if (route == null) {
            String path = ObjectHelper.supplyIfEmpty(serviceDefinition.getPath(), () -> KnativeHttpTransport.DEFAULT_PATH);
            if (ObjectHelper.isNotEmpty(basePath)) {
                path = basePath + path;
            }

            LOGGER.debug("Creating route for path: {}", path);

            route = router.route(
                HttpMethod.POST,
                path
            );

            // add body handler
            route.handler(router.bodyHandler());

            // add knative handler
            route.handler(routingContext -> {
                LOGGER.debug("Handling {}", routingContext);

                if (filter.test(routingContext.request())) {
                    handleRequest(routingContext);
                } else {
                    LOGGER.debug("Cannot handle request on {}, next", getEndpoint().getEndpointUri());
                    routingContext.next();
                }
            });
        }

        super.doStart();
    }

    @Override
    protected void doStop() throws Exception {
        if (route != null) {
            route.remove();
        }

        super.doStop();
    }

    @Override
    protected void doSuspend() throws Exception {
        if (route != null) {
            route.disable();
        }
    }

    @Override
    protected void doResume() throws Exception {
        if (route != null) {
            route.enable();
        }
    }

    private void handleRequest(RoutingContext routingContext) {
        final HttpServerRequest request = routingContext.request();
        final Exchange exchange = getEndpoint().createExchange();
        final Message message = toMessage(request, exchange);

        Buffer payload = routingContext.getBody();
        if (payload != null) {
            message.setBody(payload.getBytes());
        } else {
            message.setBody(null);
        }

        try {
            createUoW(exchange);

            // We do not know if any of the processing logic of the route is synchronous or not so we
            // need to process the request on a thread on the Vert.x worker pool.
            //
            // As example the following route may block the Vert.x event loop as the camel-http component
            // is not async so if the service is scaled-down, the it may take a while to become ready and
            // the camel-http component blocks until the service becomes available.
            //
            // from("knative:event/my.event")
            //        .to("http://{{env:PROJECT}}.{{env:NAMESPACE}}.svc.cluster.local/service");
            //
            router.vertx().executeBlocking(
                promise -> {
                    try {
                        // no need to use an async processor as the processing happen in
                        // a dedicated thread ans it won't block the Vert.x event loop
                        getProcessor().process(exchange);
                        promise.complete();
                    } catch (Exception e) {
                        promise.fail(e);
                    }
                },
                false,
                result -> {
                    if (result.succeeded()) {
                        try {
                            HttpServerResponse response = toHttpResponse(request, exchange.getMessage());
                            Buffer body = null;

                            if (request.response().getStatusCode() != 204 && configuration.isReply()) {
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
                    } else if (result.failed()) {
                        getExceptionHandler().handleException(result.cause());

                        request.response().setStatusCode(500);
                        request.response().putHeader(Exchange.CONTENT_TYPE, "text/plain");
                        request.response().end(result.cause().getMessage());
                    }

                    doneUoW(exchange);
                });
        } catch (Exception e) {
            getExceptionHandler().handleException(e);

            request.response().setStatusCode(500);
            request.response().putHeader(Exchange.CONTENT_TYPE, "text/plain");
            request.response().end(e.getMessage());

            doneUoW(exchange);
        }
    }

    private Message toMessage(HttpServerRequest request, Exchange exchange) {
        Message message = exchange.getMessage();
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

        if (configuration.isReply()) {
            for (Map.Entry<String, Object> entry : message.getHeaders().entrySet()) {
                final String key = entry.getKey();
                final Object value = entry.getValue();

                for (Object it : org.apache.camel.support.ObjectHelper.createIterable(value, null)) {
                    String headerValue = tc.convertTo(String.class, it);
                    if (headerValue == null) {
                        continue;
                    }
                    if (!headerFilterStrategy.applyFilterToCamelHeaders(key, headerValue, message.getExchange())) {
                        response.putHeader(key, headerValue);
                    }
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

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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import io.undertow.client.ClientExchange;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.TypeConverter;
import org.apache.camel.spi.HeaderFilterStrategy;
import org.apache.camel.support.DefaultMessage;
import org.apache.camel.support.ExchangeHelper;
import org.apache.camel.support.MessageHelper;
import org.apache.camel.support.ObjectHelper;
import org.apache.camel.util.IOHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.channels.BlockingReadableByteChannel;
import org.xnio.channels.StreamSourceChannel;

public final class KnativeHttpBinding {
    private static final Logger LOG = LoggerFactory.getLogger(KnativeHttpBinding.class);

    private final HeaderFilterStrategy headerFilterStrategy;
    private final Boolean transferException;

    public KnativeHttpBinding(HeaderFilterStrategy headerFilterStrategy) {
        this(headerFilterStrategy, Boolean.FALSE);
    }

    public KnativeHttpBinding(HeaderFilterStrategy headerFilterStrategy, Boolean transferException) {
        this.headerFilterStrategy = Objects.requireNonNull(headerFilterStrategy, "headerFilterStrategy");
        this.transferException = transferException;
    }

    public Message toCamelMessage(HttpServerExchange httpExchange, Exchange exchange) throws Exception {
        Message result = new DefaultMessage(exchange.getContext());

        populateCamelHeaders(httpExchange, result.getHeaders(), exchange);

        //extract body by myself if undertow parser didn't handle and the method is allowed to have one
        //body is extracted as byte[] then auto TypeConverter kicks in
        if (Methods.POST.equals(httpExchange.getRequestMethod()) || Methods.PUT.equals(httpExchange.getRequestMethod()) || Methods.PATCH.equals(httpExchange.getRequestMethod())) {
            result.setBody(readFromChannel(httpExchange.getRequestChannel()));
        } else {
            result.setBody(null);
        }

        return result;
    }

    public Message toCamelMessage(ClientExchange clientExchange, Exchange exchange) throws Exception {
        Message result = new DefaultMessage(exchange.getContext());

        //retrieve response headers
        populateCamelHeaders(clientExchange.getResponse(), result.getHeaders(), exchange);

        result.setBody(readFromChannel(clientExchange.getResponseChannel()));

        return result;
    }

    public void populateCamelHeaders(HttpServerExchange httpExchange, Map<String, Object> headersMap, Exchange exchange) {
        String path = httpExchange.getRequestPath();
        KnativeHttpEndpoint endpoint = (KnativeHttpEndpoint) exchange.getFromEndpoint();
        if (endpoint.getHttpURI() != null) {
            // need to match by lower case as we want to ignore case on context-path
            String endpointPath = endpoint.getHttpURI().getPath();
            String matchPath = path.toLowerCase(Locale.US);
            String match = endpointPath.toLowerCase(Locale.US);
            if (matchPath.startsWith(match)) {
                path = path.substring(endpointPath.length());
            }
        }
        headersMap.put(Exchange.HTTP_PATH, path);

        for (HttpString name : httpExchange.getRequestHeaders().getHeaderNames()) {
            if (name.toString().toLowerCase(Locale.US).equals("content-type")) {
                name = Headers.CONTENT_TYPE;
            }

            if (name.toString().toLowerCase(Locale.US).equals("authorization")) {
                String value = httpExchange.getRequestHeaders().get(name).toString();
                // store a special header that this request was authenticated using HTTP Basic
                if (value != null && value.trim().startsWith("Basic")) {
                    if (!headerFilterStrategy.applyFilterToExternalHeaders(Exchange.AUTHENTICATION, "Basic", exchange)) {
                        appendHeader(headersMap, Exchange.AUTHENTICATION, "Basic");
                    }
                }
            }

            // add the headers one by one, and use the header filter strategy
            for (Object value : httpExchange.getRequestHeaders().get(name)) {
                if (!headerFilterStrategy.applyFilterToExternalHeaders(name.toString(), value, exchange)) {
                    appendHeader(headersMap, name.toString(), value);
                }
            }
        }

        //process uri parameters as headers
        Map<String, Deque<String>> pathParameters = httpExchange.getQueryParameters();
        for (Map.Entry<String, Deque<String>> entry : pathParameters.entrySet()) {
            String name = entry.getKey();
            for (Object value: entry.getValue()) {
                if (!headerFilterStrategy.applyFilterToExternalHeaders(name, value, exchange)) {
                    appendHeader(headersMap, name, value);
                }
            }
        }

        headersMap.put(Exchange.HTTP_METHOD, httpExchange.getRequestMethod().toString());
        headersMap.put(Exchange.HTTP_URL, httpExchange.getRequestURL());
        headersMap.put(Exchange.HTTP_URI, httpExchange.getRequestURI());
        headersMap.put(Exchange.HTTP_QUERY, httpExchange.getQueryString());
        headersMap.put(Exchange.HTTP_RAW_QUERY, httpExchange.getQueryString());
    }

    public void populateCamelHeaders(ClientResponse response, Map<String, Object> headersMap, Exchange exchange) throws Exception {
        headersMap.put(Exchange.HTTP_RESPONSE_CODE, response.getResponseCode());

        for (HttpString name : response.getResponseHeaders().getHeaderNames()) {
            // mapping the content-type
            //String name = httpName.toString();
            if (name.toString().toLowerCase(Locale.US).equals("content-type")) {
                name = Headers.CONTENT_TYPE;
            }

            if (name.toString().toLowerCase(Locale.US).equals("authorization")) {
                String value = response.getResponseHeaders().get(name).toString();
                // store a special header that this request was authenticated using HTTP Basic
                if (value != null && value.trim().startsWith("Basic")) {
                    if (!headerFilterStrategy.applyFilterToExternalHeaders(Exchange.AUTHENTICATION, "Basic", exchange)) {
                        appendHeader(headersMap, Exchange.AUTHENTICATION, "Basic");
                    }
                }
            }

            // add the headers one by one, and use the header filter strategy
            for (Object value : response.getResponseHeaders().get(name)) {
                if (!headerFilterStrategy.applyFilterToExternalHeaders(name.toString(), value, exchange)) {
                    appendHeader(headersMap, name.toString(), value);
                }
            }
        }
    }

    public Object toHttpResponse(HttpServerExchange httpExchange, Message message) throws IOException {
        final boolean failed = message.getExchange().isFailed();
        final int defaultCode = failed ? 500 : 200;
        final int code = message.getHeader(Exchange.HTTP_RESPONSE_CODE, defaultCode, int.class);
        final TypeConverter tc = message.getExchange().getContext().getTypeConverter();

        httpExchange.setStatusCode(code);

        //copy headers from Message to Response
        for (Map.Entry<String, Object> entry : message.getHeaders().entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            // use an iterator as there can be multiple values. (must not use a delimiter)
            for (Object it: ObjectHelper.createIterable(value, null)) {
                String headerValue = tc.convertTo(String.class, it);
                if (headerValue == null) {
                    continue;
                }
                if (!headerFilterStrategy.applyFilterToCamelHeaders(key, headerValue, message.getExchange())) {
                    httpExchange.getResponseHeaders().add(new HttpString(key), headerValue);
                }
            }
        }

        Object body = message.getBody();
        Exception exception = message.getExchange().getException();

        if (exception != null) {
            if (transferException) {
                // we failed due an exception, and transfer it as java serialized object
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos);
                oos.writeObject(exception);
                oos.flush();
                IOHelper.close(oos, bos);

                // the body should be the serialized java object of the exception
                body = ByteBuffer.wrap(bos.toByteArray());
                // force content type to be serialized java object
                message.setHeader(Exchange.CONTENT_TYPE, "application/x-java-serialized-object");
            } else {
                // we failed due an exception so print it as plain text
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                exception.printStackTrace(pw);

                // the body should then be the stacktrace
                body = ByteBuffer.wrap(sw.toString().getBytes());
                // force content type to be text/plain as that is what the stacktrace is
                message.setHeader(Exchange.CONTENT_TYPE, "text/plain");
            }

            // and mark the exception as failure handled, as we handled it by returning it as the response
            ExchangeHelper.setFailureHandled(message.getExchange());
        }

        // set the content type in the response.
        String contentType = MessageHelper.getContentType(message);
        if (contentType != null) {
            // set content-type
            httpExchange.getResponseHeaders().put(Headers.CONTENT_TYPE, contentType);
            LOG.trace("Content-Type: {}", contentType);
        }
        return body;
    }

    public Object toHttpRequest(ClientRequest clientRequest, Message message) {
        final Object body = message.getBody();
        final HeaderMap requestHeaders = clientRequest.getRequestHeaders();

        // set the content type in the response.
        String contentType = MessageHelper.getContentType(message);
        if (contentType != null) {
            // set content-type
            requestHeaders.put(Headers.CONTENT_TYPE, contentType);
        }

        TypeConverter tc = message.getExchange().getContext().getTypeConverter();

        //copy headers from Message to Request
        for (Map.Entry<String, Object> entry : message.getHeaders().entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            // use an iterator as there can be multiple values. (must not use a delimiter)
            for (Object it: ObjectHelper.createIterable(value, null)) {
                String headerValue = tc.convertTo(String.class, it);
                if (headerValue == null) {
                    continue;
                }
                if (!headerFilterStrategy.applyFilterToCamelHeaders(key, headerValue, message.getExchange())) {
                    requestHeaders.add(new HttpString(key), headerValue);
                }
            }
        }

        return body;
    }

    @SuppressWarnings("unchecked")
    public static void appendHeader(Map<String, Object> headers, String key, Object value) {
        if (headers.containsKey(key)) {
            Object existing = headers.get(key);
            List<Object> list;
            if (existing instanceof List) {
                list = (List<Object>) existing;
            } else {
                list = new ArrayList<>();
                list.add(existing);
            }
            list.add(value);
            value = list;
        }

        headers.put(key, value);
    }

    public static byte[] readFromChannel(StreamSourceChannel source) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final ByteBuffer buffer = ByteBuffer.wrap(new byte[1024]);
        final ReadableByteChannel blockingSource = new BlockingReadableByteChannel(source);

        for (;;) {
            int res = blockingSource.read(buffer);
            if (res == -1) {
                return out.toByteArray();
            } else if (res == 0) {
                LOG.error("Channel did not block");
            } else {
                buffer.flip();
                out.write(
                  buffer.array(),
                  buffer.arrayOffset() + buffer.position(),
                  buffer.arrayOffset() + buffer.limit());
                buffer.clear();
            }
        }
    }
}

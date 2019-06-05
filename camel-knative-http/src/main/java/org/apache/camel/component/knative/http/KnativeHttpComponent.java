/**
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

import java.net.URI;
import java.nio.channels.ClosedChannelException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.NoTypeConversionAvailableException;
import org.apache.camel.TypeConverter;
import org.apache.camel.component.netty4.NettyConverter;
import org.apache.camel.component.netty4.http.DefaultNettyHttpBinding;
import org.apache.camel.component.netty4.http.HttpServerConsumerChannelFactory;
import org.apache.camel.component.netty4.http.NettyHttpBinding;
import org.apache.camel.component.netty4.http.NettyHttpComponent;
import org.apache.camel.component.netty4.http.NettyHttpConfiguration;
import org.apache.camel.component.netty4.http.NettyHttpConsumer;
import org.apache.camel.component.netty4.http.NettyHttpHelper;
import org.apache.camel.component.netty4.http.handlers.HttpServerChannelHandler;
import org.apache.camel.spi.HeaderFilterStrategy;
import org.apache.camel.support.RestConsumerContextPathMatcher;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.ServiceHelper;
import org.apache.camel.util.URISupport;
import org.apache.camel.util.UnsafeUriCharactersEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class KnativeHttpComponent extends NettyHttpComponent {
    private static final Logger LOGGER = LoggerFactory.getLogger(KnativeHttpComponent.class);
    private final Map<Integer, HttpServerConsumerChannelFactory> handlers = new ConcurrentHashMap<>();

    public KnativeHttpComponent() {
        super();
        setNettyHttpBinding(new KnativeNettyHttpBinding(getHeaderFilterStrategy()));
    }

    @Override
    public synchronized HttpServerConsumerChannelFactory getMultiplexChannelHandler(int port) {
        return handlers.computeIfAbsent(port, Handler::new);
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

        ServiceHelper.startService(handlers.values());
        handlers.clear();
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        return super.createEndpoint(uri, remaining, parameters);
    }

    @ChannelHandler.Sharable
    private static class Handler extends SimpleChannelInboundHandler<Object> implements HttpServerConsumerChannelFactory {
        private static final Logger LOG = LoggerFactory.getLogger(Handler.class);
        private static final AttributeKey<HttpServerChannelHandler> SERVER_HANDLER_KEY = AttributeKey.valueOf("serverHandler");

        private final Set<HttpServerChannelHandler> consumers;
        private final int port;
        private final String token;
        private final int len;

        public Handler(int port) {
            this.consumers = new CopyOnWriteArraySet<>();
            this.port = port;
            this.token = ":" + port;
            this.len = token.length();
        }

        public void init(int port) {
        }

        public void addConsumer(NettyHttpConsumer consumer) {
            consumers.add(new HttpServerChannelHandler(consumer));
        }

        public void removeConsumer(NettyHttpConsumer consumer) {
            consumers.removeIf(h -> h.getConsumer() == consumer);
        }

        public int consumers() {
            return consumers.size();
        }

        public int getPort() {
            return port;
        }

        public ChannelHandler getChannelHandler() {
            return this;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
            // store request, as this channel handler is created per pipeline
            HttpRequest request = (HttpRequest) msg;

            LOG.debug("Message received: {}", request);

            HttpServerChannelHandler handler = getHandler(request, request.method().name());
            if (handler != null) {
                Attribute<HttpServerChannelHandler> attr = ctx.channel().attr(SERVER_HANDLER_KEY);
                // store handler as attachment
                attr.set(handler);
                if (msg instanceof HttpContent) {
                    // need to hold the reference of content
                    HttpContent httpContent = (HttpContent) msg;
                    httpContent.content().retain();
                }
                handler.channelRead(ctx, request);
            } else {
                HttpResponse response = new DefaultHttpResponse(HTTP_1_1, NOT_FOUND);
                response.headers().set(Exchange.CONTENT_TYPE, "text/plain");
                response.headers().set(Exchange.CONTENT_LENGTH, 0);
                ctx.writeAndFlush(response);
                ctx.close();
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            Attribute<HttpServerChannelHandler> attr = ctx.channel().attr(SERVER_HANDLER_KEY);
            HttpServerChannelHandler handler = attr.get();
            if (handler != null) {
                handler.exceptionCaught(ctx, cause);
            } else {
                if (cause instanceof ClosedChannelException) {
                    // The channel is closed so we do nothing here
                    LOG.debug("Channel already closed. Ignoring this exception.");
                    return;
                } else {
                    // we cannot throw the exception here
                    LOG.warn("HttpServerChannelHandler is not found as attachment to handle exception, send 404 back to the client.", cause);
                    // Now we just send 404 back to the client
                    HttpResponse response = new DefaultHttpResponse(HTTP_1_1, NOT_FOUND);
                    response.headers().set(Exchange.CONTENT_TYPE, "text/plain");
                    response.headers().set(Exchange.CONTENT_LENGTH, 0);
                    ctx.writeAndFlush(response);
                    ctx.close();
                }
            }
        }

        private boolean isHttpMethodAllowed(HttpRequest request, String method) {
            return getHandler(request, method) != null;
        }

        @SuppressWarnings("unchecked")
        private HttpServerChannelHandler getHandler(HttpRequest request, String method)  {
            HttpServerChannelHandler answer = null;

            // need to strip out host and port etc, as we only need the context-path for matching
            if (method == null) {
                return null;
            }

            String path = request.uri();
            int idx = path.indexOf(token);
            if (idx > -1) {
                path = path.substring(idx + len);
            }
            // use the path as key to find the consumer handler to use
            path = pathAsKey(path);

            // fallback to regular matching
            if (answer == null) {
                for (final HttpServerChannelHandler handler : consumers) {
                    try {
                        final NettyHttpConsumer consumer = handler.getConsumer();
                        final HttpHeaders headers = request.headers();
                        final String uri = consumer.getEndpoint().getEndpointUri();
                        final Map<String, Object> params = URISupport.parseParameters(URI.create(uri));

                        if (params.containsKey("filter.headerName") && params.containsKey("filter.headerValue")) {
                            final String filterKey = (String) params.get("filter.headerName");
                            final String filterVal = (String) params.get("filter.headerValue");
                            final String headerVal = headers.get(filterKey);

                            if (ObjectHelper.isEmpty(headerVal)) {
                                continue;
                            }
                            if (!ObjectHelper.equal(filterVal, headerVal)) {
                                continue;
                            }
                        }

                        String consumerPath = consumer.getConfiguration().getPath();
                        boolean matchOnUriPrefix = consumer.getEndpoint().getConfiguration().isMatchOnUriPrefix();
                        // Just make sure the we get the right consumer path first
                        if (RestConsumerContextPathMatcher.matchPath(path, consumerPath, matchOnUriPrefix)) {
                            answer = handler;
                            break;
                        }
                    } catch (Exception e) {
                        throw ObjectHelper.wrapRuntimeCamelException(e);
                    }
                }
            }

            return answer;
        }

        private static String pathAsKey(String path) {
            // cater for default path
            if (path == null || path.equals("/")) {
                path = "";
            }

            // strip out query parameters
            int idx = path.indexOf('?');
            if (idx > -1) {
                path = path.substring(0, idx);
            }

            // strip of ending /
            if (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }

            return UnsafeUriCharactersEncoder.encodeHttpURI(path);
        }

    }


    /**
     * Default {@link NettyHttpBinding}.
     */
    public class KnativeNettyHttpBinding extends DefaultNettyHttpBinding {
        public KnativeNettyHttpBinding(HeaderFilterStrategy headerFilterStrategy) {
            super(headerFilterStrategy);
        }

        @Override
        public HttpRequest toNettyRequest(Message message, String uri, NettyHttpConfiguration configuration) throws Exception {
            LOGGER.trace("toNettyRequest: {}", message);

            // the message body may already be a Netty HTTP response
            if (message.getBody() instanceof HttpRequest) {
                return (HttpRequest) message.getBody();
            }

            String uriForRequest = uri;
            if (configuration.isUseRelativePath()) {
                uriForRequest = URISupport.pathAndQueryOf(new URI(uriForRequest));
            }

            // just assume GET for now, we will later change that to the actual method to use
            HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uriForRequest);

            Object body = message.getBody();
            if (body != null) {
                // support bodies as native Netty
                ByteBuf buffer;
                if (body instanceof ByteBuf) {
                    buffer = (ByteBuf) body;
                } else {
                    // try to convert to buffer first
                    buffer = message.getBody(ByteBuf.class);
                    if (buffer == null) {
                        // fallback to byte array as last resort
                        byte[] data = message.getMandatoryBody(byte[].class);
                        buffer = NettyConverter.toByteBuffer(data);
                    }
                }
                if (buffer != null) {
                    request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, uriForRequest, buffer);
                    int len = buffer.readableBytes();
                    // set content-length
                    request.headers().set(HttpHeaderNames.CONTENT_LENGTH.toString(), len);
                    LOGGER.trace("Content-Length: {}", len);
                } else {
                    // we do not support this kind of body
                    throw new NoTypeConversionAvailableException(body, ByteBuf.class);
                }
            }

            // update HTTP method accordingly as we know if we have a body or not
            HttpMethod method = NettyHttpHelper.createMethod(message, body != null);
            request.setMethod(method);

            TypeConverter tc = message.getExchange().getContext().getTypeConverter();

            // if we bridge endpoint then we need to skip matching headers with the HTTP_QUERY to avoid sending
            // duplicated headers to the receiver, so use this skipRequestHeaders as the list of headers to skip
            Map<String, Object> skipRequestHeaders = null;
            if (configuration.isBridgeEndpoint()) {
                String queryString = message.getHeader(Exchange.HTTP_QUERY, String.class);
                if (queryString != null) {
                    skipRequestHeaders = URISupport.parseQuery(queryString, false, true);
                }
                // Need to remove the Host key as it should be not used
                message.getHeaders().remove("host");
            }

            // append headers
            // must use entrySet to ensure case of keys is preserved
            for (Map.Entry<String, Object> entry : message.getHeaders().entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                // we should not add headers for the parameters in the uri if we bridge the endpoint
                // as then we would duplicate headers on both the endpoint uri, and in HTTP headers as well
                if (skipRequestHeaders != null && skipRequestHeaders.containsKey(key)) {
                    continue;
                }

                // use an iterator as there can be multiple values. (must not use a delimiter)
                final Iterator<?> it = ObjectHelper.createIterator(value, null, true);
                while (it.hasNext()) {
                    String headerValue = tc.convertTo(String.class, it.next());

                    if (headerValue != null && getHeaderFilterStrategy() != null
                        && !getHeaderFilterStrategy().applyFilterToCamelHeaders(key, headerValue, message.getExchange())) {
                        LOGGER.trace("HTTP-Header: {}={}", key, headerValue);
                        request.headers().add(key, headerValue);
                    }
                }
            }

            // set the content type in the response.
            String contentType = message.getHeader(Exchange.CONTENT_TYPE, String.class);
            if (contentType != null) {
                // set content-type
                request.headers().set(HttpHeaderNames.CONTENT_TYPE.toString(), contentType);
                LOGGER.trace("Content-Type: {}", contentType);
            }

            // must include HOST header as required by HTTP 1.1
            // use URI as its faster than URL (no DNS lookup)
            URI u = new URI(uri);
            String hostHeader = u.getHost() + (u.getPort() == 80 ? "" : ":" + u.getPort());
            request.headers().set(HttpHeaderNames.HOST.toString(), hostHeader);
            LOGGER.trace("Host: {}", hostHeader);

            // configure connection to accordingly to keep alive configuration
            // favor using the header from the message
            String connection = message.getHeader(HttpHeaderNames.CONNECTION.toString(), String.class);
            if (connection == null) {
                // fallback and use the keep alive from the configuration
                if (configuration.isKeepAlive()) {
                    connection = HttpHeaderValues.KEEP_ALIVE.toString();
                } else {
                    connection = HttpHeaderValues.CLOSE.toString();
                }
            }
            request.headers().set(HttpHeaderNames.CONNECTION.toString(), connection);
            LOGGER.trace("Connection: {}", connection);

            return request;
        }
    }
}

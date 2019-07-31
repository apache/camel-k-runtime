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

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.predicate.Predicate;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.ReferenceCount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class KnativeHttpDispatcher implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(KnativeHttpDispatcher.class);

    private final KnativeHttp.HostKey key;
    private final KnativeHttp.HostOptions options;
    private final ReferenceCount refCnt;
    private final Set<PredicatedHandlerWrapper> handlers;
    private final Undertow undertow;
    private final PathHandler handler;

    public KnativeHttpDispatcher(KnativeHttp.HostKey key, KnativeHttp.HostOptions option) {
        this.handlers = new CopyOnWriteArraySet<>();
        this.key = key;
        this.options = option;
        this.handler = new PathHandler(this);
        this.undertow = createUndertow();
        this.refCnt = ReferenceCount.on(this::startUndertow, this::stopUndertow);
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        LOGGER.debug("received exchange on path: {}, headers: {}",
            exchange.getRelativePath(),
            exchange.getRequestHeaders()
        );

        for (PredicatedHandlerWrapper handler: handlers) {
            if (handler.dispatch(exchange)) {
                return;
            }
        }

        exchange.setStatusCode(StatusCodes.NOT_FOUND);
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
        exchange.getResponseSender().send("No matching condition found");
    }

    public void bind(HttpHandler handler, Predicate predicate) {
        if (handlers.add(new PredicatedHandlerWrapper(handler, predicate))) {
            refCnt.retain();
        }
    }

    public void unbind(HttpHandler handler) {
        if (handlers.removeIf(phw -> phw.handler == handler)) {
            refCnt.release();
        }
    }

    private void startUndertow() {
        try {
            LOGGER.info("Starting Undertow server on {}://{}:{}}",
              key.getSslContext() != null ? "https" : "http",
              key.getHost(),
              key.getPort()
            );

            undertow.start();
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to start Undertow server on {}://{}:{}, reason: {}",
              key.getSslContext() != null ? "https" : "http",
              key.getHost(),
              key.getPort(),
              e.getMessage()
            );

            undertow.stop();

            throw e;
        }
    }

    private void stopUndertow() {
        LOGGER.info("Stopping Undertow server on {}://{}:{}",
          key.getSslContext() != null ? "https" : "http",
          key.getHost(),
          key.getPort());

        undertow.stop();
    }

    private Undertow createUndertow() {
        Undertow.Builder builder = Undertow.builder();
        if (key.getSslContext() != null) {
            builder.addHttpsListener(key.getPort(), key.getHost(), key.getSslContext());
        } else {
            builder.addHttpListener(key.getPort(), key.getHost());
        }

        if (options != null) {
            ObjectHelper.ifNotEmpty(options.getIoThreads(), builder::setIoThreads);
            ObjectHelper.ifNotEmpty(options.getWorkerThreads(), builder::setWorkerThreads);
            ObjectHelper.ifNotEmpty(options.getBufferSize(), builder::setBufferSize);
            ObjectHelper.ifNotEmpty(options.getDirectBuffers(), builder::setDirectBuffers);
            ObjectHelper.ifNotEmpty(options.getHttp2Enabled(), e -> builder.setServerOption(UndertowOptions.ENABLE_HTTP2, e));
        }

        return builder.setHandler(new PathHandler(handler)).build();
    }

    private static final class PredicatedHandlerWrapper {
        private final HttpHandler handler;
        private final Predicate predicate;

        public PredicatedHandlerWrapper(HttpHandler handler, Predicate predicate) {
            this.handler = ObjectHelper.notNull(handler, "handler");
            this.predicate = ObjectHelper.notNull(predicate, "predicate");
        }

        boolean dispatch(HttpServerExchange exchange) throws Exception {
            if (predicate.resolve(exchange)) {
                if (exchange.isInIoThread()) {
                    exchange.dispatch(handler);
                } else {
                    handler.handleRequest(exchange);
                }

                return true;
            }

            LOGGER.debug("No handler for path: {}, headers: {}",
                exchange.getRelativePath(),
                exchange.getRequestHeaders()
            );

            return false;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            PredicatedHandlerWrapper holder = (PredicatedHandlerWrapper) o;
            return handler.equals(holder.handler);
        }

        @Override
        public int hashCode() {
            return Objects.hash(handler);
        }
    }
}

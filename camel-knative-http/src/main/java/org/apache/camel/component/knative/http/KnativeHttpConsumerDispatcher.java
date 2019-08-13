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

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import org.apache.camel.Exchange;
import org.apache.camel.support.service.ServiceSupport;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.ReferenceCount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class KnativeHttpConsumerDispatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(KnativeHttpConsumerDispatcher.class);

    private final Vertx vertx;
    private final KnativeHttp.ServerKey key;
    private final ReferenceCount refCnt;
    private final Set<KnativeHttp.PredicatedHandler> handlers;
    private final HttpServerWrapper server;
    private final HttpServerOptions serverOptions;
    private final ExecutorService executor;

    public KnativeHttpConsumerDispatcher(ExecutorService executor,  Vertx vertx, KnativeHttp.ServerKey key, HttpServerOptions serverOptions) {
        this.executor = executor;
        this.vertx = vertx;
        this.serverOptions = ObjectHelper.supplyIfEmpty(serverOptions, HttpServerOptions::new);
        this.server = new HttpServerWrapper();

        this.handlers = new CopyOnWriteArraySet<>();
        this.key = key;
        this.refCnt = ReferenceCount.on(server::start, server::stop);
    }

    public void bind(KnativeHttp.PredicatedHandler handler) {
        if (handlers.add(handler)) {
            refCnt.retain();
        }
    }

    public void unbind(KnativeHttp.PredicatedHandler handler) {
        if (handlers.remove(handler)) {
            refCnt.release();
        }
    }

    private final class HttpServerWrapper extends ServiceSupport implements Handler<HttpServerRequest> {
        private HttpServer server;

        @Override
        protected void doStart() throws Exception {
            LOGGER.info("Starting Vert.x HttpServer on {}:{}}",
                key.getHost(),
                key.getPort()
            );

            startAsync().toCompletableFuture().join();
        }

        @Override
        protected void doStop() throws Exception {
            LOGGER.info("Stopping Vert.x HttpServer on {}:{}",
                key.getHost(),
                key.getPort());

            try {
                if (server != null) {
                    stopAsync().toCompletableFuture().join();
                }
            } finally {
                this.server = null;
            }
        }

        private CompletionStage<Void> startAsync() {
            server = vertx.createHttpServer(serverOptions);
            server.requestHandler(this);

            return CompletableFuture.runAsync(
                () -> {
                    CountDownLatch latch = new CountDownLatch(1);

                    server.listen(key.getPort(), key.getHost(), result -> {
                        try {
                            if (result.failed()) {
                                LOGGER.warn("Failed to start Vert.x HttpServer on {}:{}, reason: {}",
                                    key.getHost(),
                                    key.getPort(),
                                    result.cause().getMessage()
                                );

                                throw new RuntimeException(result.cause());
                            }

                            LOGGER.info("Vert.x HttpServer started on {}:{}", key.getPort(), key.getHost());
                        } finally {
                            latch.countDown();
                        }
                    });

                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                },
                executor
            );
        }

        protected CompletionStage<Void> stopAsync() {
            return CompletableFuture.runAsync(
                () -> {
                    CountDownLatch latch = new CountDownLatch(1);

                    server.close(result -> {
                        try {
                            if (result.failed()) {
                                LOGGER.warn("Failed to close Vert.x HttpServer reason: {}",
                                    result.cause().getMessage()
                                );

                                throw new RuntimeException(result.cause());
                            }

                            LOGGER.info("Vert.x HttpServer stopped");
                        } finally {
                            latch.countDown();
                        }
                    });

                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                },
                executor
            );
        }

        @Override
        public void handle(HttpServerRequest request) {
            LOGGER.debug("received exchange on path: {}, headers: {}",
                request.path(),
                request.headers()
            );

            for (KnativeHttp.PredicatedHandler handler: handlers) {
                if (handler.canHandle(request)) {
                    handler.handle(request);
                    return;
                }
            }

            LOGGER.warn("No handler found for path: {}, headers: {}",
                request.path(),
                request.headers()
            );

            HttpServerResponse response = request.response();
            response.setStatusCode(404);
            response.putHeader(Exchange.CONTENT_TYPE, "text/plain");
            response.end("No matching condition found");
        }
    }
}

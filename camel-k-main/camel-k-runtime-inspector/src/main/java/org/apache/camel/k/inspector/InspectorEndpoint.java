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
package org.apache.camel.k.inspector;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import org.apache.camel.CamelContext;
import org.apache.camel.support.CamelContextHelper;
import org.apache.camel.support.service.ServiceSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InspectorEndpoint extends ServiceSupport {
    private static final Logger LOGGER = LoggerFactory.getLogger(InspectorEndpoint.class);

    private final CamelContext context;
    private final String bindHost;
    private final int bindPort;
    private final String path;

    private Vertx vertx;
    private boolean localVertx;
    private ExecutorService executor;
    private HttpServerWrapper vertxHttpServer;

    public InspectorEndpoint(CamelContext context, String bindHost, int bindPort, String path) {
        this.context = context;
        this.bindHost = bindHost;
        this.bindPort = bindPort;
        this.path = path;
    }

    @Override
    protected void doStart() throws Exception {
        this.executor = context.getExecutorServiceManager().newSingleThreadExecutor(this, "main-actuator");
        this.vertx = CamelContextHelper.findByType(context, Vertx.class);

        if (this.vertx != null) {
            LOGGER.info("Found Vert.x instance in registry: {}", this.vertx);
        } else {
            VertxOptions options = CamelContextHelper.findByType(context, VertxOptions.class);
            if (options == null) {
                options = new VertxOptions();
            }

            LOGGER.info("Creating new Vert.x instance");

            this.vertx = Vertx.vertx(options);
            this.localVertx = true;
        }

        vertxHttpServer = new HttpServerWrapper();
        vertxHttpServer.start();
    }

    @Override
    protected void doStop() throws Exception {
        if (this.vertxHttpServer != null) {
            vertxHttpServer.stop();
        }

        if (this.vertx != null && this.localVertx) {
            Future<?> future = this.executor.submit(
                () -> {
                    CountDownLatch latch = new CountDownLatch(1);

                    this.vertx.close(result -> {
                        try {
                            if (result.failed()) {
                                LOGGER.warn("Failed to close Vert.x reason: {}",
                                    result.cause().getMessage()
                                );

                                throw new RuntimeException(result.cause());
                            }

                            LOGGER.info("Vert.x stopped");
                        } finally {
                            latch.countDown();
                        }
                    });

                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            );

            try {
                future.get();
            } finally {
                this.vertx = null;
                this.localVertx = false;
            }
        }

        if (this.executor != null) {
            context.getExecutorServiceManager().shutdownNow(this.executor);
        }
    }

    private final class HttpServerWrapper extends ServiceSupport {
        private HttpServer server;

        @Override
        protected void doStart() throws Exception {
            startAsync().toCompletableFuture().join();
        }

        @Override
        protected void doStop() throws Exception {
            try {
                if (server != null) {
                    stopAsync().toCompletableFuture().join();
                }
            } finally {
                this.server = null;
            }
        }

        private CompletionStage<Void> startAsync() {
            final Router router = Router.router(vertx);
            final Router subRouter = Router.router(vertx);

            context.getRegistry().findByType(InspectorCustomizer.class).forEach(customizer -> {
                LOGGER.debug("InspectorCustomizer: {}", customizer);
                customizer.accept(subRouter);
            });

            router.mountSubRouter(path, subRouter);

            server = vertx.createHttpServer();
            return CompletableFuture.runAsync(
                () -> {
                    CountDownLatch latch = new CountDownLatch(1);

                    server.requestHandler(router).listen(bindPort, bindHost, result -> {
                        try {
                            if (result.failed()) {
                                LOGGER.warn("Failed to start Vert.x HttpServer on {}:{}, reason: {}",
                                    bindHost,
                                    bindPort,
                                    result.cause().getMessage()
                                );

                                throw new RuntimeException(result.cause());
                            }

                            LOGGER.info("Vert.x HttpServer started on {}:{}", bindHost, bindPort);
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
    }
}

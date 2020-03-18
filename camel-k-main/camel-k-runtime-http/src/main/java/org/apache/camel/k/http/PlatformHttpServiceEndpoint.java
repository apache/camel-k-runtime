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
package org.apache.camel.k.http;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import org.apache.camel.CamelContext;
import org.apache.camel.support.CamelContextHelper;
import org.apache.camel.support.service.ServiceSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlatformHttpServiceEndpoint extends ServiceSupport {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformHttpServiceEndpoint.class);

    private final CamelContext context;
    private final PlatformHttpServiceConfiguration configuration;

    private Vertx vertx;
    private boolean localVertx;
    private PlatformHttpServer vertxHttpServer;
    private ExecutorService executor;

    public PlatformHttpServiceEndpoint(CamelContext context, PlatformHttpServiceConfiguration configuration) {
        this.context = context;
        this.configuration = configuration;
    }

    @Override
    protected void doStart() throws Exception {
        vertx = CamelContextHelper.findByType(context, Vertx.class);
        executor = context.getExecutorServiceManager().newSingleThreadExecutor(this, "platform-http-service");

        if (vertx != null) {
            LOGGER.info("Found Vert.x instance in registry: {}", vertx);
        } else {
            VertxOptions options = CamelContextHelper.findByType(context, VertxOptions.class);
            if (options == null) {
                options = new VertxOptions();
            }

            LOGGER.info("Creating new Vert.x instance");

            vertx = Vertx.vertx(options);
            localVertx = true;
        }

        vertxHttpServer = new PlatformHttpServer(context, configuration, vertx, executor);
        vertxHttpServer.start();
    }

    @Override
    protected void doStop() throws Exception {
        if (vertxHttpServer != null) {
            vertxHttpServer.stop();
        }

        if (vertx != null && localVertx) {
            Future<?> future = executor.submit(
                () -> {
                    CountDownLatch latch = new CountDownLatch(1);

                    vertx.close(result -> {
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
                vertx = null;
                localVertx = false;
            }
        }

        if (executor != null) {
            context.getExecutorServiceManager().shutdown(executor);
            executor = null;
        }
    }
}

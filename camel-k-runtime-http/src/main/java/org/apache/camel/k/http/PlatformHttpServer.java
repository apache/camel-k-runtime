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

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.TrustOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.apache.camel.CamelContext;
import org.apache.camel.component.platform.http.PlatformHttpConstants;
import org.apache.camel.support.jsse.KeyManagersParameters;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.support.jsse.TrustManagersParameters;
import org.apache.camel.support.service.ServiceSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PlatformHttpServer extends ServiceSupport {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformHttpServer.class);

    private final CamelContext context;
    private final PlatformHttpServiceConfiguration configuration;
    private final Vertx vertx;
    private final ExecutorService executor;

    private HttpServer server;

    public PlatformHttpServer(CamelContext context, PlatformHttpServiceConfiguration configuration, Vertx vertx, ExecutorService executor) {
        this.context = context;
        this.configuration = configuration;
        this.vertx = vertx;
        this.executor = executor;
    }

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

        router.mountSubRouter(configuration.getPath(), subRouter);

        context.getRegistry().bind(
            PlatformHttp.PLATFORM_HTTP_ROUTER_NAME,
            new PlatformHttp(vertx, subRouter, Collections.singletonList(createBodyHandler()))
        );

        SSLContextParameters sslParameters = configuration.getSslContextParameters();
        if (sslParameters == null && configuration.isUseGlobalSslContextParameters()) {
            sslParameters = context.getSSLContextParameters();
        }

        HttpServerOptions options = new HttpServerOptions();
        if (sslParameters != null) {
            options.setSsl(true);
            options.setKeyCertOptions(createKeyCertOptions(sslParameters));
            options.setTrustOptions(createTrustOptions(sslParameters));
        }

        server = vertx.createHttpServer(options);

        return CompletableFuture.runAsync(
            () -> {
                CountDownLatch latch = new CountDownLatch(1);
                server.requestHandler(router).listen(configuration.getBindPort(), configuration.getBindHost(), result -> {
                    try {
                        if (result.failed()) {
                            LOGGER.warn("Failed to start Vert.x HttpServer on {}:{}, reason: {}",
                                configuration.getBindHost(),
                                configuration.getBindPort(),
                                result.cause().getMessage()
                            );

                            throw new RuntimeException(result.cause());
                        }

                        LOGGER.info("Vert.x HttpServer started on {}:{}", configuration.getBindHost(), configuration.getBindPort());
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

                // remove the platform-http component
                context.removeComponent(PlatformHttpConstants.PLATFORM_HTTP_COMPONENT_NAME);

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

    private Handler<RoutingContext> createBodyHandler() {
        BodyHandler bodyHandler = BodyHandler.create();

        if (configuration.getMaxBodySize() != null) {
            bodyHandler.setBodyLimit(configuration.getMaxBodySize().longValueExact());
        }

        bodyHandler.setHandleFileUploads(configuration.getBodyHandler().isHandleFileUploads());
        bodyHandler.setUploadsDirectory(configuration.getBodyHandler().getUploadsDirectory());
        bodyHandler.setDeleteUploadedFilesOnEnd(configuration.getBodyHandler().isDeleteUploadedFilesOnEnd());
        bodyHandler.setMergeFormAttributes(configuration.getBodyHandler().isMergeFormAttributes());
        bodyHandler.setPreallocateBodyBuffer(configuration.getBodyHandler().isPreallocateBodyBuffer());

        return new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext event) {
                event.request().resume();
                bodyHandler.handle(event);
            }
        };
    }

    // *****************************
    //
    // SSL
    //
    // *****************************

    private KeyCertOptions createKeyCertOptions(SSLContextParameters sslContextParameters) {
        return new KeyCertOptions() {
            @Override
            public KeyManagerFactory getKeyManagerFactory(Vertx vertx) throws Exception {
                return createKeyManagerFactory(sslContextParameters);
            }

            @Override
            public KeyCertOptions clone() {
                return this;
            }
        };
    }

    private KeyManagerFactory createKeyManagerFactory(SSLContextParameters sslContextParameters) throws GeneralSecurityException, IOException {
        final KeyManagersParameters keyManagers = sslContextParameters.getKeyManagers();
        if (keyManagers == null) {
            return null;
        }

        String kmfAlgorithm = context.resolvePropertyPlaceholders(keyManagers.getAlgorithm());
        if (kmfAlgorithm == null) {
            kmfAlgorithm = KeyManagerFactory.getDefaultAlgorithm();
        }

        KeyManagerFactory kmf;
        if (keyManagers.getProvider() == null) {
            kmf = KeyManagerFactory.getInstance(kmfAlgorithm);
        } else {
            kmf = KeyManagerFactory.getInstance(kmfAlgorithm, context.resolvePropertyPlaceholders(keyManagers.getProvider()));
        }

        char[] kmfPassword = null;
        if (keyManagers.getKeyPassword() != null) {
            kmfPassword = context.resolvePropertyPlaceholders(keyManagers.getKeyPassword()).toCharArray();
        }

        KeyStore ks = keyManagers.getKeyStore() == null ? null : keyManagers.getKeyStore().createKeyStore();

        kmf.init(ks, kmfPassword);
        return kmf;
    }

    private TrustOptions createTrustOptions(SSLContextParameters sslContextParameters) {
        return new TrustOptions() {
            @Override
            public TrustOptions clone() {
                return this;
            }

            @Override
            public TrustManagerFactory getTrustManagerFactory(Vertx vertx) throws Exception {
                return createTrustManagerFactory(sslContextParameters);
            }
        };
    }

    private TrustManagerFactory createTrustManagerFactory(SSLContextParameters sslContextParameters) throws GeneralSecurityException, IOException {
        final TrustManagersParameters trustManagers = sslContextParameters.getTrustManagers();
        if (trustManagers == null) {
            return null;
        }

        TrustManagerFactory tmf = null;

        if (trustManagers.getKeyStore() != null) {
            String tmfAlgorithm = context.resolvePropertyPlaceholders(trustManagers.getAlgorithm());
            if (tmfAlgorithm == null) {
                tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            }

            if (trustManagers.getProvider() == null) {
                tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            } else {
                tmf = TrustManagerFactory.getInstance(tmfAlgorithm, context.resolvePropertyPlaceholders(trustManagers.getProvider()));
            }

            KeyStore ks = trustManagers.getKeyStore() == null ? null : trustManagers.getKeyStore().createKeyStore();
            tmf.init(ks);
        }

        return tmf;
    }
}

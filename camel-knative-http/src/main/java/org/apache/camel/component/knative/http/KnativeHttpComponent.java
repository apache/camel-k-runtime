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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.regex.Matcher;

import static java.lang.Integer.*;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.client.WebClientOptions;
import org.apache.camel.Endpoint;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.annotations.Component;
import org.apache.camel.support.DefaultComponent;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.PropertiesHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component("knative-http")
public class KnativeHttpComponent extends DefaultComponent {
    private static final Logger LOGGER = LoggerFactory.getLogger(KnativeHttpComponent.class);

    private final Map<KnativeHttp.ServerKey, KnativeHttpConsumerDispatcher> registry;

    @Metadata(label = "advanced")
    private Vertx vertx;
    @Metadata(label = "advanced")
    private VertxOptions vertxOptions;
    @Metadata(label = "advanced")
    private HttpServerOptions vertxHttpServerOptions;
    @Metadata(label = "advanced")
    private WebClientOptions vertxHttpClientOptions;

    private boolean localVertx;
    private ExecutorService executor;

    public KnativeHttpComponent() {
        this.registry = new ConcurrentHashMap<>();
        this.localVertx = false;
    }

    @Override
    protected void doInit() throws Exception {
        super.doInit();

        this.executor = getCamelContext().getExecutorServiceManager().newSingleThreadExecutor(this, "knative-http-component");

        if (this.vertx == null) {
            Set<Vertx> instances = getCamelContext().getRegistry().findByType(Vertx.class);
            if (instances.size() == 1) {
                this.vertx = instances.iterator().next();
            }
        }

        if (this.vertx == null) {
            VertxOptions options = ObjectHelper.supplyIfEmpty(this.vertxOptions, VertxOptions::new);

            this.vertx = Vertx.vertx(options);
            this.localVertx = true;
        }
    }

    @Override
    protected void doShutdown() throws Exception {
        super.doShutdown();

        if (this.vertx != null && this.localVertx) {
            Future<?> future = this.executor.submit(
                () -> {
                    CountDownLatch latch = new CountDownLatch(1);

                    this.vertx.close(result -> {
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
            getCamelContext().getExecutorServiceManager().shutdownNow(this.executor);
        }
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        Matcher matcher = KnativeHttp.ENDPOINT_PATTERN.matcher(remaining);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Bad URI: " + remaining);
        }

        KnativeHttpEndpoint ep = new KnativeHttpEndpoint(uri, this);
        ep.setHeaderFilter(PropertiesHelper.extractProperties(parameters, "filter.", true));

        switch (matcher.groupCount()) {
        case 1:
            ep.setHost(matcher.group(1));
            ep.setPort(KnativeHttp.DEFAULT_PORT);
            ep.setPath(KnativeHttp.DEFAULT_PATH);
            break;
        case 2:
            ep.setHost(matcher.group(1));
            ep.setPort(parseInt(matcher.group(2)));
            ep.setPath(KnativeHttp.DEFAULT_PATH);
            break;
        case 3:
            ep.setHost(matcher.group(1));
            ep.setPort(parseInt(matcher.group(2)));
            ep.setPath(KnativeHttp.DEFAULT_PATH + matcher.group(3));
            break;
        default:
            throw new IllegalArgumentException("Bad URI: " + remaining);
        }

        setProperties(ep, parameters);

        return ep;
    }

    public Vertx getVertx() {
        return vertx;
    }

    public void setVertx(Vertx vertx) {
        this.vertx = vertx;
    }

    public VertxOptions getVertxOptions() {
        return vertxOptions;
    }

    public void setVertxOptions(VertxOptions vertxOptions) {
        this.vertxOptions = vertxOptions;
    }

    public HttpServerOptions getVertxHttpServerOptions() {
        return vertxHttpServerOptions;
    }

    public void setVertxHttpServerOptions(HttpServerOptions vertxHttpServerOptions) {
        this.vertxHttpServerOptions = vertxHttpServerOptions;
    }

    public WebClientOptions getVertxHttpClientOptions() {
        return vertxHttpClientOptions;
    }

    public void setVertxHttpClientOptions(WebClientOptions vertxHttpClientOptions) {
        this.vertxHttpClientOptions = vertxHttpClientOptions;
    }

    KnativeHttpConsumerDispatcher getDispatcher(KnativeHttp.ServerKey key) {
        return registry.computeIfAbsent(key, k -> new KnativeHttpConsumerDispatcher(executor, vertx, k, vertxHttpServerOptions));
    }

    ExecutorService getExecutorService() {
        return this.executor;
    }
}

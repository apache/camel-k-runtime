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
package org.apache.camel.component.kamelet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.VetoCamelContextStartException;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.annotations.Component;
import org.apache.camel.support.DefaultComponent;
import org.apache.camel.support.LifecycleStrategySupport;
import org.apache.camel.support.service.ServiceHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Kamelet Component provides support for materializing routes templates.
 */
@Component(Kamelet.SCHEME)
public class KameletComponent extends DefaultComponent {
    private static final Logger LOGGER = LoggerFactory.getLogger(KameletComponent.class);

    private final Map<String, KameletConsumer> consumers;
    private final LifecycleHandler lifecycleHandler;

    @Metadata(label = "producer", defaultValue = "true")
    private boolean block = true;
    @Metadata(label = "producer", defaultValue = "30000")
    private long timeout = 30000L;

    public KameletComponent() {
        this.lifecycleHandler = new LifecycleHandler();
        this.consumers = new ConcurrentHashMap<>();
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        final String templateId = Kamelet.extractTemplateId(getCamelContext(), remaining);
        final String routeId = Kamelet.extractRouteId(getCamelContext(), remaining);
        final String newUri = "kamelet:" + templateId + "/" + routeId;

        final KameletEndpoint endpoint;

        if (!Kamelet.SOURCE_ID.equals(remaining) && !Kamelet.SINK_ID.equals(remaining)) {
            endpoint = new KameletEndpoint(newUri, this, templateId, routeId, consumers) {
                @Override
                protected void doInit() throws Exception {
                    super.doInit();
                    lifecycleHandler.track(this);
                }
            };

            // forward component properties
            endpoint.setBlock(block);
            endpoint.setTimeout(timeout);

            // set endpoint specific properties
            setProperties(endpoint, parameters);

            //
            // The properties for the kamelets are determined by global properties
            // and local endpoint parameters,
            //
            // Global parameters are loaded in the following order:
            //
            //   camel.kamelet." + templateId
            //   camel.kamelet." + templateId + "." routeId
            //
            Map<String, Object> kameletProperties = Kamelet.extractKameletProperties(getCamelContext(), templateId, routeId);
            kameletProperties.putAll(parameters);
            kameletProperties.put("templateId", templateId);
            kameletProperties.put("routeId", routeId);

            // set kamelet specific properties
            endpoint.setKameletProperties(kameletProperties);
        } else {
            endpoint = new KameletEndpoint(newUri, this, templateId, routeId, consumers);

            // forward component properties
            endpoint.setBlock(block);
            endpoint.setTimeout(timeout);

            // set endpoint specific properties
            setProperties(endpoint, parameters);
        }

        return endpoint;
    }

    public boolean isBlock() {
        return block;
    }

    /**
     * If sending a message to a kamelet endpoint which has no active consumer, then we can tell the producer to block
     * and wait for the consumer to become active.
     */
    public void setBlock(boolean block) {
        this.block = block;
    }

    public long getTimeout() {
        return timeout;
    }

    /**
     * The timeout value to use if block is enabled.
     */
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    @Override
    protected void doInit() throws Exception {
        getCamelContext().addLifecycleStrategy(lifecycleHandler);

        if (getCamelContext().isRunAllowed()) {
            lifecycleHandler.setInitialized(true);
        }

        super.doInit();
    }

    @Override
    protected void doStop() throws Exception {
        getCamelContext().getLifecycleStrategies().remove(lifecycleHandler);

        ServiceHelper.stopService(consumers.values());
        consumers.clear();

        super.doStop();
    }

    /*
     * This LifecycleHandler is used to keep track of created kamelet endpoints during startup as
     * we need to defer create routes from templates until camel context has finished loading
     * all routes and whatnot.
     *
     * Once the camel context is initialized all the endpoint tracked by this LifecycleHandler will
     * be used to create routes from templates.
     */
    private static class LifecycleHandler extends LifecycleStrategySupport {
        private final List<KameletEndpoint> endpoints;
        private final AtomicBoolean initialized;

        public LifecycleHandler() {
            this.endpoints = new ArrayList<>();
            this.initialized = new AtomicBoolean();
        }

        @Override
        public void onContextInitialized(CamelContext context) throws VetoCamelContextStartException {
            if (!this.initialized.compareAndExchange(false, true)) {
                ModelCamelContext mcc = context.adapt(ModelCamelContext.class);

                for (KameletEndpoint endpoint : endpoints) {
                    try {
                        createRouteForEndpoint(endpoint);
                    } catch (Exception e) {
                        throw new VetoCamelContextStartException("Failure creating route from template: " + endpoint.getTemplateId(), e, context);
                    }
                }

                endpoints.clear();
            }
        }

        public void setInitialized(boolean initialized) {
            this.initialized.set(initialized);
        }

        public void track(KameletEndpoint endpoint) {
            if (this.initialized.get()) {
                try {
                    createRouteForEndpoint(endpoint);
                } catch (Exception e) {
                    throw RuntimeCamelException.wrapRuntimeException(e);
                }
            } else {
                LOGGER.debug("Tracking route template={} and id={}", endpoint.getTemplateId(), endpoint.getRouteId());
                this.endpoints.add(endpoint);
            }
        }

        public static void createRouteForEndpoint(KameletEndpoint endpoint) throws Exception {
            LOGGER.debug("Creating route from template={} and id={}", endpoint.getTemplateId(), endpoint.getRouteId());

            final ModelCamelContext context = endpoint.getCamelContext().adapt(ModelCamelContext.class);
            final String id = context.addRouteFromTemplate(endpoint.getRouteId(), endpoint.getTemplateId(), endpoint.getKameletProperties());
            final RouteDefinition def = context.getRouteDefinition(id);

            if (!def.isPrepared()) {
                context.startRouteDefinitions(List.of(def));
            }

            LOGGER.debug("Route with id={} created from template={}", id, endpoint.getTemplateId());
        }
    }
}

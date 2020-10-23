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
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.VetoCamelContextStartException;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.PropertiesComponent;
import org.apache.camel.spi.annotations.Component;
import org.apache.camel.support.DefaultComponent;
import org.apache.camel.support.LifecycleStrategySupport;
import org.apache.camel.support.service.ServiceHelper;
import org.apache.camel.util.StringHelper;
import org.apache.camel.util.URISupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.camel.component.kamelet.Kamelet.PARAM_ROUTE_ID;
import static org.apache.camel.component.kamelet.Kamelet.PARAM_TEMPLATE_ID;
import static org.apache.camel.component.kamelet.Kamelet.addRouteFromTemplate;

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
        final String templateId = Kamelet.extractTemplateId(getCamelContext(), remaining, parameters);
        final String routeId = Kamelet.extractRouteId(getCamelContext(), remaining, parameters);

        parameters.remove(PARAM_TEMPLATE_ID);
        parameters.remove(PARAM_ROUTE_ID);

        final KameletEndpoint endpoint;

        if (Kamelet.SOURCE_ID.equals(remaining) || Kamelet.SINK_ID.equals(remaining)) {
            //
            // if remaining is either `source` or `sink' then it is a virtual
            // endpoint that is used inside the kamelet definition to mark it
            // as in/out endpoint.
            //
            // The following snippet defines a template which will act as a
            // consumer for this Kamelet:
            //
            //     from("kamelet:source")
            //         .to("log:info")
            //
            // The following snippet defines a template which will act as a
            // producer for this Kamelet:
            //
            //     from("telegram:bots")
            //         .to("kamelet:sink")
            //
            // Note that at the moment, there's no enforcement around `source`
            // and `sink' to be defined on the right side (producer or consumer)
            //
            endpoint = new KameletEndpoint(uri, this, templateId, routeId, consumers);

            // forward component properties
            endpoint.setBlock(block);
            endpoint.setTimeout(timeout);

            // set endpoint specific properties
            setProperties(endpoint, parameters);
        } else {
            endpoint = new KameletEndpoint(uri, this, templateId, routeId, consumers) {
                @Override
                protected void doInit() throws Exception {
                    super.doInit();
                    //
                    // since this is the real kamelet, then we need to hand it
                    // over to the tracker.
                    //
                    lifecycleHandler.track(this);
                }
            };

            // forward component properties
            endpoint.setBlock(block);
            endpoint.setTimeout(timeout);

            // set endpoint specific properties
            setProperties(endpoint, parameters);

            // determine the parameters that the kamelet should take by using the original
            // uri as we need to preserve the original format.
            final String query = StringHelper.after(uri, "?");
            final Map<String, Object> queryParams = URISupport.parseQuery(query, true, true);

            // replace resolved params with the original ones
            parameters.replaceAll(queryParams::getOrDefault);

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
            kameletProperties.put(PARAM_TEMPLATE_ID, templateId);
            kameletProperties.put(PARAM_ROUTE_ID, routeId);

            // set kamelet specific properties
            endpoint.setKameletProperties(kameletProperties);
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
    public boolean useRawUri() {
        // should use encoded uri by default
        return true;
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
            final String id = addRouteFromTemplate(context, endpoint.getRouteId(), endpoint.getTemplateId(), endpoint.getKameletProperties());
            final RouteDefinition def = context.getRouteDefinition(id);

            if (!def.isPrepared()) {
                // when starting the route that was created from the template
                // then we must provide the route id as local properties to the
                // properties component as this route id is used internal by
                // kamelets when they are starting
                PropertiesComponent pc = context.getPropertiesComponent();
                try {
                    Properties prop = new Properties();
                    prop.put(PARAM_TEMPLATE_ID, endpoint.getTemplateId());
                    prop.put(PARAM_ROUTE_ID, id);
                    pc.setLocalProperties(prop);
                    context.startRouteDefinitions(List.of(def));
                } finally {
                    pc.setLocalProperties(null);
                }
            }

            LOGGER.debug("Route with id={} created from template={}", id, endpoint.getTemplateId());
        }
    }
}

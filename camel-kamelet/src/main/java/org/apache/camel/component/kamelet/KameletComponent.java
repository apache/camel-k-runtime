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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.spi.CamelEvent;
import org.apache.camel.spi.annotations.Component;
import org.apache.camel.support.DefaultComponent;
import org.apache.camel.support.EventNotifierSupport;
import org.apache.camel.support.service.ServiceHelper;
import org.apache.camel.util.StringHelper;

@Component(Kamelet.SCHEME)
public class KameletComponent extends DefaultComponent {
    public KameletComponent() {
        this(null);
    }

    public KameletComponent(CamelContext context) {
        super(context);
    }

    // use as temporary to keep track of created kamelet endpoints during startup as we need to defer
    // create routes from templates until camel context has finished loading all routes and whatnot
    private final List<KameletEndpoint> endpoints = new ArrayList<>();
    private volatile RouteTemplateEventNotifier notifier;

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        final String templateId = extractTemplateId(remaining);
        final String routeId = extractRouteId(remaining);

        //
        // The properties for the kamelets are determined by global properties
        // and local endpoint parameters,
        //
        // Global parameters are loaded in the following order:
        //
        //   camel.kamelet." + templateId
        //   camel.kamelet." + templateId + "." routeId
        //
        Map<String, Object> kameletProperties = extractKameletProperties(templateId, routeId);
        kameletProperties.putAll(parameters);
        kameletProperties.putIfAbsent("templateId", templateId);
        kameletProperties.putIfAbsent("routeId", routeId);

        // Remaining parameter should be related to the route and to avoid the
        // parameters validation to fail, we need to clear the parameters map.
        parameters.clear();

        KameletEndpoint endpoint = new KameletEndpoint(uri, this, templateId, routeId, kameletProperties);

        // No parameters are expected here.
        setProperties(endpoint, parameters);

        return endpoint;
    }

    private String extractTemplateId(String remaining) {
        String answer = StringHelper.before(remaining, "/");
        if (answer == null) {
            answer = remaining;
        }

        return answer;
    }

    private String extractRouteId(String remaining) {
        String answer = StringHelper.after(remaining, "/");
        if (answer == null) {
            answer = extractTemplateId(remaining) + "-" + getCamelContext().getUuidGenerator().generateUuid();
        }

        return answer;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractKameletProperties(String... elements) {
        Map<String, Object> properties = new HashMap<>();
        String prefix = "camel.kamelet.";

        for (String element: elements) {
            if (element == null) {
                continue;
            }

            prefix = prefix + element + ".";

            properties.putAll(
                (Map)getCamelContext().getPropertiesComponent().loadProperties(Kamelet.startsWith(prefix))
            );

        }

        return properties;
    }

    @Override
    protected void doInit() throws Exception {
        super.doInit();

        if (!getCamelContext().isRunAllowed()) {
            // setup event listener which must be started to get triggered during initialization of camel context
            notifier = new RouteTemplateEventNotifier(this);
            ServiceHelper.startService(notifier);
            getCamelContext().getManagementStrategy().addEventNotifier(notifier);
        }
    }

    @Override
    protected void doStop() throws Exception {
        if (notifier != null) {
            ServiceHelper.stopService(notifier);
            getCamelContext().getManagementStrategy().removeEventNotifier(notifier);
            notifier = null;
        }
        super.doStop();
    }

    void onEndpointAdd(KameletEndpoint endpoint) {
        if (notifier == null) {
            try {
                addRouteFromTemplate(endpoint);
            } catch (Exception e) {
                throw RuntimeCamelException.wrapRuntimeException(e);
            }
        } else {
            // remember endpoints as we defer adding routes for them till later
            this.endpoints.add(endpoint);
        }
    }

    void addRouteFromTemplate(KameletEndpoint endpoint) throws Exception {
        ModelCamelContext context = endpoint.getCamelContext().adapt(ModelCamelContext.class);
        String id = context.addRouteFromTemplate(endpoint.getRouteId(), endpoint.getTemplateId(), endpoint.getKameletProperties());
        RouteDefinition def = context.getRouteDefinition(id);
        if (!def.isPrepared()) {
            List<RouteDefinition> list = new ArrayList<>(1);
            list.add(def);
            context.startRouteDefinitions(list);
        }
    }

    private static class RouteTemplateEventNotifier extends EventNotifierSupport {

        private final KameletComponent component;

        public RouteTemplateEventNotifier(KameletComponent component) {
            this.component = component;
        }

        @Override
        public void notify(CamelEvent event) throws Exception {
            for (KameletEndpoint endpoint : component.endpoints) {
                component.addRouteFromTemplate(endpoint);
            }
            component.endpoints.clear();
            // we were only needed during initializing/starting up camel, so remove after use
            ServiceHelper.stopService(this);
            component.getCamelContext().getManagementStrategy().removeEventNotifier(this);
            component.notifier = null;
        }

        @Override
        public boolean isEnabled(CamelEvent event) {
            // we only care about this event during startup as its triggered when
            // all route and route template definitions have been added and prepared
            // so this allows us to hook into the right moment
            return event instanceof CamelEvent.CamelContextInitializedEvent;
        }

    }
}

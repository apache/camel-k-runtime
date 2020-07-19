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

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.spi.annotations.Component;
import org.apache.camel.support.DefaultComponent;
import org.apache.camel.util.StringHelper;

@Component(Kamelet.SCHEME)
public class KameletComponent extends DefaultComponent {
    public KameletComponent() {
        this(null);
    }

    public KameletComponent(CamelContext context) {
        super(context);
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        final String templateId = extractTemplateId(remaining);
        final String routeId = extractRouteId(remaining);

        //
        // The properties for the kamelets are determined by global properties
        // and local endpoint parametes,
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
}

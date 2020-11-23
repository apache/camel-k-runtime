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
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.StringJoiner;
import java.util.function.Predicate;

import org.apache.camel.CamelContext;
import org.apache.camel.model.FromDefinition;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.RouteTemplateDefinition;
import org.apache.camel.model.RouteTemplateParameterDefinition;
import org.apache.camel.model.ToDefinition;
import org.apache.camel.spi.PropertiesComponent;
import org.apache.camel.support.CamelContextHelper;
import org.apache.camel.util.StringHelper;

import static org.apache.camel.model.ProcessorDefinitionHelper.filterTypeInOutputs;

public final class Kamelet {
    public static final String PROPERTIES_PREFIX = "camel.kamelet.";
    public static final String SCHEME = "kamelet";
    public static final String SOURCE_ID = "source";
    public static final String SINK_ID = "sink";
    public static final String PARAM_ROUTE_ID = "routeId";
    public static final String PARAM_TEMPLATE_ID = "templateId";

    private Kamelet() {
    }

    public static Predicate<String> startsWith(String prefix) {
        return item -> item.startsWith(prefix);
    }

    public static String extractTemplateId(CamelContext context, String remaining, Map<String, Object> parameters) {
        Object param = parameters.get(PARAM_TEMPLATE_ID);
        if (param != null) {
            return CamelContextHelper.mandatoryConvertTo(context, String.class, param);
        }

        if (SOURCE_ID.equals(remaining) || SINK_ID.equals(remaining)) {
            return context.resolvePropertyPlaceholders("{{" + PARAM_TEMPLATE_ID + "}}");
        }

        String answer = StringHelper.before(remaining, "/");
        if (answer == null) {
            answer = remaining;
        }

        return answer;
    }

    public static String extractRouteId(CamelContext context, String remaining, Map<String, Object> parameters) {
        Object param = parameters.get(PARAM_ROUTE_ID);
        if (param != null) {
            return CamelContextHelper.mandatoryConvertTo(context, String.class, param);
        }

        if (SOURCE_ID.equals(remaining) || SINK_ID.equals(remaining)) {
            return context.resolvePropertyPlaceholders("{{" + PARAM_ROUTE_ID + "}}");
        }

        String answer = StringHelper.after(remaining, "/");
        if (answer == null) {
            answer = extractTemplateId(context, remaining, parameters) + "-" + context.getUuidGenerator().generateUuid();
        }

        return answer;
    }

    public static Map<String, Object> extractKameletProperties(CamelContext context, Map<String, Object> properties, String... elements) {
        PropertiesComponent pc = context.getPropertiesComponent();
        String prefix = Kamelet.PROPERTIES_PREFIX;

        for (String element: elements) {
            if (element == null) {
                continue;
            }

            prefix = prefix + element + ".";

            Properties prefixed = pc.loadProperties(Kamelet.startsWith(prefix));
            for (String name : prefixed.stringPropertyNames()) {
                properties.put(name.substring(prefix.length()), prefixed.getProperty(name));
            }
        }

        return properties;
    }

    public static String addRouteFromTemplate(ModelCamelContext context, String routeId, String routeTemplateId, Map<String, Object> parameters) throws Exception {
        RouteTemplateDefinition target = null;
        for (RouteTemplateDefinition def : context.getRouteTemplateDefinitions()) {
            if (routeTemplateId.equals(def.getId())) {
                target = def;
                break;
            }
        }
        if (target == null) {
            throw new IllegalArgumentException("Cannot find RouteTemplate with id " + routeTemplateId);
        }

        StringJoiner templatesBuilder = new StringJoiner(", ");
        final Map<String, Object> prop = new HashMap<>();
        // include default values first from the template (and validate that we have inputs for all required parameters)
        if (target.getTemplateParameters() != null) {
            for (RouteTemplateParameterDefinition temp : target.getTemplateParameters()) {
                if (temp.getDefaultValue() != null) {
                    prop.put(temp.getName(), temp.getDefaultValue());
                } else {
                    // this is a required parameter do we have that as input
                    if (!parameters.containsKey(temp.getName())) {
                        templatesBuilder.add(temp.getName());
                    }
                }
            }
        }
        if (templatesBuilder.length() > 0) {
            throw new IllegalArgumentException(
                "Route template " + routeTemplateId + " the following mandatory parameters must be provided: "
                    + templatesBuilder.toString());
        }
        // then override with user parameters
        if (parameters != null) {
            prop.putAll(parameters);
        }

        RouteDefinition def = target.asRouteDefinition();
        // must make deep copy of input
        def.setInput(null);
        def.setInput(new FromDefinition(target.getRoute().getInput().getEndpointUri()));
        if (routeId != null) {
            def.setId(routeId);
        }
        // must make the source and sink endpoints are unique by appending the route id before we create the route from the template
        if (def.getInput().getEndpointUri().startsWith("kamelet:source") || def.getInput().getEndpointUri().startsWith("kamelet//source")) {
            def.getInput().setUri("kamelet:source?" + PARAM_ROUTE_ID + "=" + routeId);
        }
        Iterator<ToDefinition> it = filterTypeInOutputs(def.getOutputs(), ToDefinition.class);
        while (it.hasNext()) {
            ToDefinition to = it.next();
            if (to.getEndpointUri().startsWith("kamelet:sink") || to.getEndpointUri().startsWith("kamelet://sink")) {
                to.setUri("kamelet:sink?" + PARAM_ROUTE_ID + "=" + routeId);
            }
        }

        def.setTemplateParameters(prop);
        context.removeRouteDefinition(def);
        context.getRouteDefinitions().add(def);

        return def.getId();
    }

}

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
package org.apache.camel.k.loader.yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.k.RoutesLoader;
import org.apache.camel.k.Source;
import org.apache.camel.k.loader.yaml.model.Route;
import org.apache.camel.k.loader.yaml.parser.StartStepParser;
import org.apache.camel.k.loader.yaml.parser.StepParser;
import org.apache.camel.k.support.URIResolver;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.RoutesDefinition;
import org.apache.camel.model.rest.RestDefinition;
import org.apache.camel.model.rest.RestsDefinition;

public class YamlRoutesLoader implements RoutesLoader {
    @Override
    public List<String> getSupportedLanguages() {
        return Collections.singletonList("yaml");
    }

    @Override
    public RouteBuilder load(CamelContext camelContext, Source source) throws Exception {
        return builder(URIResolver.resolve(camelContext, source));
    }

    public static RouteBuilder builder(InputStream is) {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                final CamelContext camelContext = getContext();
                final List<RouteDefinition> routes = new ArrayList<>();
                final List<RestDefinition> rests = new ArrayList<>();

                try {
                    for (Route route : Yaml.MAPPER.readValue(is, Route[].class)) {
                        Route.Definition definition = route.getDefinition();

                        if (definition != null) {
                            StepParser.Context context = new StepParser.Context(camelContext, definition.getData());
                            ProcessorDefinition<?> root = StartStepParser.invoke(context, definition.getType());

                            if (root == null) {
                                throw new IllegalStateException("No route definition");
                            }
                            if (!(root instanceof RouteDefinition)) {
                                throw new IllegalStateException("Root definition should be of type RouteDefinition");
                            }

                            RouteDefinition r = (RouteDefinition) root;
                            if (r.getRestDefinition() == null) {
                                routes.add(r);
                            } else {
                                rests.add(r.getRestDefinition());
                            }

                            route.getId().ifPresent(root::routeId);
                            route.getGroup().ifPresent(root::routeGroup);
                        }
                    }

                    if (!routes.isEmpty()) {
                        RoutesDefinition definition = new RoutesDefinition();
                        definition.setRoutes(routes);

                        setRouteCollection(definition);
                    }
                    if (!rests.isEmpty()) {
                        RestsDefinition definition = new RestsDefinition();
                        definition.setRests(rests);

                        setRestCollection(definition);
                    }
                } finally {
                    is.close();
                }
            }
        };
    }
}

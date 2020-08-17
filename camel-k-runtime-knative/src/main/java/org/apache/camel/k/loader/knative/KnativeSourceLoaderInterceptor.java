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
package org.apache.camel.k.loader.knative;

import java.util.List;
import java.util.Optional;

import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.k.Source;
import org.apache.camel.k.SourceLoader;
import org.apache.camel.k.annotation.LoaderInterceptor;
import org.apache.camel.k.support.RuntimeSupport;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.ToDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@LoaderInterceptor("knative-source")
public class KnativeSourceLoaderInterceptor implements SourceLoader.Interceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(KnativeSourceLoaderInterceptor.class);

    @Override
    public void beforeLoad(SourceLoader loader, Source source) {
        // no-op
    }

    @Override
    public SourceLoader.Result afterLoad(SourceLoader loader, Source source, SourceLoader.Result result) {
        return new SourceLoader.Result() {
            @Override
            public Optional<RoutesBuilder> builder() {
                return RuntimeSupport.afterConfigure(result.builder(), builder -> {
                    final CamelContext camelContext = builder.getContext();
                    final List<RouteDefinition> definitions = builder.getRouteCollection().getRoutes();

                    if (definitions.size() == 1) {
                        final String sinkName = camelContext.resolvePropertyPlaceholders("{{knative.sink:sink}}");
                        final String sinkUri = String.format("knative://endpoint/%s", sinkName);
                        final RouteDefinition definition = definitions.get(0);

                        LOGGER.info("Add sink:{} to route:{}", sinkUri, definition.getId());

                        // assuming that route is linear like there's no content based routing
                        // or ant other EIP that would branch the flow
                        definition.getOutputs().add(new ToDefinition(sinkUri));
                    } else {
                        LOGGER.warn("Cannot determine route to enrich. the knative enpoint need to explicitly be defined");
                    }
                });
            }

            @Override
            public Optional<Object> configuration() {
                return result.configuration();
            }
        };
    }

}

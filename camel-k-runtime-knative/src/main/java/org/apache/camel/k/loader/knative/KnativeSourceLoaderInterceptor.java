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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.component.knative.spi.Knative;
import org.apache.camel.component.knative.spi.KnativeEnvironment;
import org.apache.camel.k.Source;
import org.apache.camel.k.SourceLoader;
import org.apache.camel.k.annotation.LoaderInterceptor;
import org.apache.camel.k.support.RuntimeSupport;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.ToDefinition;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@LoaderInterceptor("knative-source")
public class KnativeSourceLoaderInterceptor implements SourceLoader.Interceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(KnativeSourceLoaderInterceptor.class);

    @Override
    public void beforeLoad(SourceLoader loader, Source source) {
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

                        createSyntheticDefinition(camelContext, sinkName).ifPresent(serviceDefinition -> {
                            // publish the synthetic service definition
                            camelContext.getRegistry().bind(sinkName, serviceDefinition);
                        });

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

    private static Optional<KnativeEnvironment.KnativeServiceDefinition> createSyntheticDefinition(
            CamelContext camelContext,
            String sinkName) {

        final String kSinkUrl = camelContext.resolvePropertyPlaceholders("{{k.sink:}}");
        final String kCeOverride = camelContext.resolvePropertyPlaceholders("{{k.ce.overrides:}}");

        if (ObjectHelper.isNotEmpty(kSinkUrl)) {
            // create a synthetic service definition to target the K_SINK url
            var serviceBuilder = KnativeEnvironment.serviceBuilder(Knative.Type.endpoint, sinkName)
                .withMeta(Knative.CAMEL_ENDPOINT_KIND, Knative.EndpointKind.sink)
                .withMeta(Knative.SERVICE_META_URL, kSinkUrl);

            if (ObjectHelper.isNotEmpty(kCeOverride)) {
                try (Reader reader = new StringReader(kCeOverride)) {
                    // assume K_CE_OVERRIDES is defined as simple key/val json
                    var overrides = Knative.MAPPER.readValue(
                        reader,
                        new TypeReference<HashMap<String, String>>() { }
                    );

                    for (var entry: overrides.entrySet()) {
                        // generate proper ce-override meta-data for the service
                        // definition
                        serviceBuilder.withMeta(
                            Knative.KNATIVE_CE_OVERRIDE_PREFIX + entry.getKey(),
                            entry.getValue()
                        );
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            return Optional.of(serviceBuilder.build());
        }

        return Optional.empty();
    }
}

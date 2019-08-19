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

import java.util.Arrays;
import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.engine.AbstractCamelContext;
import org.apache.camel.k.RoutesLoader;
import org.apache.camel.k.Source;
import org.apache.camel.k.support.RuntimeSupport;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.ToDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KnativeSourceRoutesLoader implements RoutesLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(KnativeSourceRoutesLoader.class);
    private static final String LOADER_ID = "knative-source";
    private static final String LANGUAGE_PREFIX = LOADER_ID + "-";

    @Override
    public List<String> getSupportedLanguages() {
        return Arrays.asList(LANGUAGE_PREFIX + "yaml");
    }

    @Override
    public RouteBuilder load(CamelContext camelContext, Source source) throws Exception {
        if (LOADER_ID.equals(source.getLanguage())) {
            throw new IllegalArgumentException("Cannot load source of type " +  source.getLanguage());
        }

        String languageId = source.getLanguage();
        if (languageId.startsWith(LANGUAGE_PREFIX)) {
            languageId = languageId.substring(LANGUAGE_PREFIX.length());
        }

        final RoutesLoader loader = RuntimeSupport.lookupLoaderByLanguage(camelContext, languageId);
        final RouteBuilder builder = loader.load(camelContext, source);

        return new RouteBuilder() {
            @Override
            public void setContext(CamelContext context) {
                builder.setContext(context);
            }

            @Override
            public void configure() throws Exception {
            }

            @Override
            public void addRoutesToCamelContext(CamelContext context) throws Exception {
                //TODO: this is a little hack as then configureRoutes will
                //      be invoked twice: 1 by this hack and 1 by delegated
                //      builder. Maybe, we should add builder lifecycle events.
                List<RouteDefinition> definitions = builder.configureRoutes(context).getRoutes();

                if (definitions.size() == 1) {
                    final String sink = context.resolvePropertyPlaceholders("{{env:KNATIVE_SINK:sink}}");
                    final String uri = String.format("knative://endpoint/%s", sink);
                    final RouteDefinition definition = definitions.get(0);

                    LOGGER.info("Add sink:{} to route:{}", uri, definition.getId());

                    // assuming that route is linear like there's no content based routing
                    // or ant other EIP that would branch the flow
                    definition.getOutputs().add(new ToDefinition(uri));
                } else {
                   LOGGER.warn("Cannot determine route to enrich. the knative enpoint need to explicitly be defined");
                }

                //TODO: this is needed for java language because by default
                //      camel main inspects route builders to detect beans
                //      to be registered to the camel registry but as the
                //      original builder is masked by this wrapping builder,
                //      beans can't be automatically discovered
                context.adapt(AbstractCamelContext.class)
                    .getBeanPostProcessor()
                    .postProcessBeforeInitialization(builder, builder.getClass().getName());

                builder.addRoutesToCamelContext(context);
            }
        };
    }
}

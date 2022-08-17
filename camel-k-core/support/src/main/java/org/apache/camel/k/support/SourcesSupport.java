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
package org.apache.camel.k.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.RouteBuilderLifecycleStrategy;
import org.apache.camel.k.Runtime;
import org.apache.camel.k.Source;
import org.apache.camel.k.SourceDefinition;
import org.apache.camel.k.listener.SourcesConfigurer;
import org.apache.camel.spi.Resource;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SourcesSupport {
    private static final Logger LOGGER = LoggerFactory.getLogger(SourcesConfigurer.class);

    private SourcesSupport() {
    }

    public static void loadSources(Runtime runtime, String... routes) {
        for (String route : routes) {
            if (ObjectHelper.isEmpty(route)) {
                continue;
            }

            LOGGER.info("Loading routes from: {}", route);

            try {
                load(runtime, Sources.fromURI(route));
            } catch (Exception e) {
                throw RuntimeCamelException.wrapRuntimeCamelException(e);
            }
        }
    }

    public static void loadSources(Runtime runtime, SourceDefinition... definitions) {
        for (SourceDefinition definition : definitions) {
            LOGGER.info("Loading routes from: {}", definition);

            load(runtime, Sources.fromDefinition(definition));
        }
    }

    public static void load(Runtime runtime, Source source) {
        try {
            final List<RouteBuilderLifecycleStrategy> interceptors = new ArrayList<>();
            interceptors.addAll(RuntimeSupport.loadInterceptors(runtime, source));
            interceptors.addAll(runtime.getCamelContext().getRegistry().findByType(RouteBuilderLifecycleStrategy.class));

            final Resource resource = Sources.asResource(runtime.getCamelContext(), source);
            final ExtendedCamelContext ecc = runtime.getCamelContext(ExtendedCamelContext.class);
            final Collection<RoutesBuilder> builders = ecc.getRoutesLoader().findRoutesBuilders(resource);

            builders.stream()
                    .map(RouteBuilder.class::cast)
                    .peek(b -> interceptors.forEach(b::addLifecycleInterceptor))
                    .forEach(runtime::addRoutes);
        } catch (Exception e) {
            throw RuntimeCamelException.wrapRuntimeCamelException(e);
        }
    }
}

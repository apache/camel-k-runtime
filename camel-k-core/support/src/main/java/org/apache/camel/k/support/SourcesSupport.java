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

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;

import org.apache.camel.ErrorHandlerFactory;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.RouteBuilderLifecycleStrategy;
import org.apache.camel.k.Runtime;
import org.apache.camel.k.RuntimeAware;
import org.apache.camel.k.Source;
import org.apache.camel.k.SourceDefinition;
import org.apache.camel.k.listener.AbstractPhaseListener;
import org.apache.camel.k.listener.SourcesConfigurer;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.RouteTemplateDefinition;
import org.apache.camel.spi.Resource;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SourcesSupport {
    private static final Logger LOGGER = LoggerFactory.getLogger(SourcesConfigurer.class);

    private SourcesSupport() {
    }

    public static Runtime.Listener forRoutes(String... sources) {
        return new AbstractPhaseListener(Runtime.Phase.ConfigureRoutes) {
            @Override
            protected void accept(Runtime runtime) {
                loadSources(runtime, sources);
            }
        };
    }

    public static Runtime.Listener forRoutes(SourceDefinition... definitions) {
        return new AbstractPhaseListener(Runtime.Phase.ConfigureRoutes) {
            @Override
            protected void accept(Runtime runtime) {
                loadSources(runtime, definitions);
            }
        };
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
        final List<RouteBuilderLifecycleStrategy> interceptors;

        switch (source.getType()) {
            case source:
                interceptors = RuntimeSupport.loadInterceptors(runtime.getCamelContext(), source);
                interceptors.forEach(interceptor -> {
                    if (interceptor instanceof RuntimeAware) {
                        ((RuntimeAware) interceptor).setRuntime(runtime);
                    }
                });

                break;
            case template:
                if (!source.getInterceptors().isEmpty()) {
                    LOGGER.warn("Interceptors associated to the route template {} will be ignored", source.getName());
                }

                interceptors = List.of(new RouteBuilderLifecycleStrategy() {
                    @Override
                    public void afterConfigure(RouteBuilder builder) {
                        List<RouteDefinition> routes = builder.getRouteCollection().getRoutes();
                        List<RouteTemplateDefinition> templates = builder.getRouteTemplateCollection().getRouteTemplates();

                        if (routes.size() != 1) {
                            throw new IllegalArgumentException(
                                "There should be a single route definition when configuring route templates, got " + routes.size());
                        }
                        if (!templates.isEmpty()) {
                            throw new IllegalArgumentException(
                                "There should not be any template definition when configuring route templates, got " + templates.size());
                        }

                        // create a new template from the source
                        RouteTemplateDefinition templatesDefinition = builder.getRouteTemplateCollection().routeTemplate(source.getId());
                        templatesDefinition.setRoute(routes.get(0));

                        source.getPropertyNames().forEach(templatesDefinition::templateParameter);

                        // remove all routes definitions as they have been translated
                        // in the related route template
                        routes.clear();
                    }
                });
                break;
            case errorHandler:
                if (!source.getInterceptors().isEmpty()) {
                    LOGGER.warn("Interceptors associated to the error handler {} will be ignored", source.getName());
                }

                interceptors = List.of(new RouteBuilderLifecycleStrategy() {
                    @Override
                    public void afterConfigure(RouteBuilder builder) {
                        List<RouteDefinition> routes = builder.getRouteCollection().getRoutes();
                        List<RouteTemplateDefinition> templates = builder.getRouteTemplateCollection().getRouteTemplates();

                        if (!routes.isEmpty()) {
                            throw new IllegalArgumentException(
                                "There should be no route definition when configuring error handler, got " + routes.size());
                        }
                        if (!templates.isEmpty()) {
                            throw new IllegalArgumentException(
                                "There should not be any template definition when configuring error handler, got " + templates.size());
                        }
                        if (hasErrorHandlerFactory(builder)){
                            LOGGER.debug("Setting default error handler builder factory as type {}", builder.getErrorHandlerFactory().getClass());
                            runtime.getCamelContext().adapt(ExtendedCamelContext.class).setErrorHandlerFactory(builder.getErrorHandlerFactory());
                        }
                    }
                });
                break;
            default:
                throw new IllegalArgumentException("Unknown source type: " + source.getType());
        }

        try {
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

    static boolean hasErrorHandlerFactory(RouteBuilder builder) {
        //return builder.hasErrorHandlerFactory();
        // TODO We need to replace the following workaround with the statement above once we switch to > camel-3.18
        try {
            Field f = RouteBuilder.class.getSuperclass().getDeclaredField("errorHandlerFactory");
            f.setAccessible(true);
            ErrorHandlerFactory privateErrorHandlerFactory = (ErrorHandlerFactory) f.get(builder);
            LOGGER.debug("Looking up for private error handler factory: {}", privateErrorHandlerFactory);
            return privateErrorHandlerFactory != null;
        } catch (Exception e) {
            throw new IllegalArgumentException("Something went wrong while checking the error handler factory", e);
        }
    }

    public static void loadErrorHandlerSource(Runtime runtime, SourceDefinition errorHandlerSourceDefinition) {
        LOGGER.info("Loading error handler from: {}", errorHandlerSourceDefinition);
        load(runtime, Sources.fromDefinition(errorHandlerSourceDefinition));
    }
}

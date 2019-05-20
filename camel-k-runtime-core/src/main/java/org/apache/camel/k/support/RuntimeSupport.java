/**
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.camel.CamelContext;
import org.apache.camel.NoFactoryAvailableException;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.k.Constants;
import org.apache.camel.k.ContextCustomizer;
import org.apache.camel.k.RoutesLoader;
import org.apache.camel.k.Runtime;
import org.apache.camel.k.Source;
import org.apache.camel.spi.FactoryFinder;
import org.apache.camel.spi.RestConfiguration;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class RuntimeSupport {
    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeSupport.class);

    private RuntimeSupport() {
    }

    public static List<ContextCustomizer> configureContext(CamelContext context, Runtime.Registry registry) {
        List<ContextCustomizer> appliedCustomizers = new ArrayList<>();
        Map<String, ContextCustomizer> customizers = lookupCustomizers(context);

        customizers.entrySet().stream()
            .sorted(Map.Entry.comparingByValue())
            .forEach(e -> {
                LOGGER.info("Apply ContextCustomizer with id={} and type={}", e.getKey(), e.getValue().getClass().getName());

                PropertiesSupport.bindProperties(context, e.getValue(), "customizer." + e.getKey() + ".");
                e.getValue().apply(context, registry);

                appliedCustomizers.add(e.getValue());
            });

        return appliedCustomizers;
    }

    @SuppressWarnings("unchecked")
    public static void configureRest(CamelContext context) {
        RestConfiguration configuration = new RestConfiguration();
        configuration.setComponentProperties(new HashMap<>());
        configuration.setEndpointProperties(new HashMap<>());

        PropertiesSupport.forEachProperty(
            context,
            name -> name.startsWith(Constants.PROPERTY_PREFIX_REST_COMPONENT_PROPERTY),
            (k, v) -> configuration.getComponentProperties().put(k.substring(Constants.PROPERTY_PREFIX_REST_COMPONENT_PROPERTY.length()), v)
        );
        PropertiesSupport.forEachProperty(
            context,
            name -> name.startsWith(Constants.PROPERTY_PREFIX_REST_ENDPOINT_PROPERTY),
            (k, v) -> configuration.getEndpointProperties().put(k.substring(Constants.PROPERTY_PREFIX_REST_ENDPOINT_PROPERTY.length()), v)
        );

        if (PropertiesSupport.bindProperties(context, configuration, "camel.rest.") > 0) {
            //
            // Set the rest configuration if only if at least one
            // rest parameter has been set.
            //
            context.setRestConfiguration(configuration);
        }
    }

    // *********************************
    //
    // Helpers - Customizers
    //
    // *********************************

    public static Map<String, ContextCustomizer> lookupCustomizers(CamelContext context) {
        Map<String, ContextCustomizer> customizers = new ConcurrentHashMap<>();

        PropertiesComponent component = context.getComponent("properties", PropertiesComponent.class);
        Properties properties = component.getInitialProperties();

        if (properties != null) {
            //
            // Lookup customizers listed in Constants.ENV_CAMEL_K_CUSTOMIZERS or Constants.PROPERTY_CAMEL_K_CUSTOMIZER
            // for backward compatibility
            //
            for (String customizerId: lookupCustomizerIDs(context)) {
                customizers.computeIfAbsent(customizerId, id -> lookupCustomizerByID(context, id));
            }

            Pattern pattern = Pattern.compile(Constants.ENABLE_CUSTOMIZER_PATTERN);

            properties.entrySet().stream()
                .filter(entry -> entry.getKey() instanceof String)
                .filter(entry -> entry.getValue() != null)
                .forEach(entry -> {
                        final String key = (String)entry.getKey();
                        final Object val = entry.getValue();
                        final Matcher matcher = pattern.matcher(key);

                        if (matcher.matches() && matcher.groupCount() == 1) {
                            if (Boolean.valueOf(String.valueOf(val))) {
                                //
                                // Do not override customizers eventually found
                                // in the registry
                                //
                                customizers.computeIfAbsent(matcher.group(1), id -> lookupCustomizerByID(context, id));
                            }
                        }
                    }
                );
        }

        return customizers;
    }

    public static ContextCustomizer lookupCustomizerByID(CamelContext context, String customizerId) {
        ContextCustomizer customizer = context.getRegistry().lookupByNameAndType(customizerId, ContextCustomizer.class);
        if (customizer == null) {
            try {
                customizer = (ContextCustomizer) context.getFactoryFinder(Constants.CONTEXT_CUSTOMIZER_RESOURCE_PATH).newInstance(customizerId);
            } catch (NoFactoryAvailableException e) {
                throw new RuntimeException(e);
            }
        }

        return customizer;
    }

    public static Set<String> lookupCustomizerIDs(CamelContext context) {
        Set<String> customizers = new TreeSet<>();

        String customizerIDs = System.getenv().getOrDefault(Constants.ENV_CAMEL_K_CUSTOMIZERS, "");
        if (ObjectHelper.isEmpty(customizerIDs)) {
            PropertiesComponent component = context.getComponent("properties", PropertiesComponent.class);
            Properties properties = component.getInitialProperties();

            if (properties != null) {
                customizerIDs = properties.getProperty(Constants.PROPERTY_CAMEL_K_CUSTOMIZER, "");
            }
        }

        if  (ObjectHelper.isNotEmpty(customizerIDs)) {
            for (String customizerId : customizerIDs.split(",", -1)) {
                customizers.add(customizerId);
            }
        }

        return customizers;
    }

    // *********************************
    //
    // Helpers - Loaders
    //
    // *********************************

    public static RoutesLoader loaderFor(CamelContext context, Source source) {
        return  context.getRegistry().findByType(RoutesLoader.class).stream()
            .filter(rl -> rl.getSupportedLanguages().contains(source.getLanguage()))
            .findFirst()
            .orElseGet(() -> lookupLoaderFromResource(context, source));
    }

    public static RoutesLoader lookupLoaderFromResource(CamelContext context, Source source) {
        final FactoryFinder finder;
        final RoutesLoader loader;

        try {
            finder = context.getFactoryFinder(Constants.ROUTES_LOADER_RESOURCE_PATH);
            loader = (RoutesLoader)finder.newInstance(source.getLanguage());
        } catch (NoFactoryAvailableException e) {
            throw new IllegalArgumentException("Unable to find loader for: " + source, e);
        }

        return loader;
    }
}

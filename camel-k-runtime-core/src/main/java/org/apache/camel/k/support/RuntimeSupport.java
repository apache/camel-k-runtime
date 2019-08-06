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
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.camel.CamelContext;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.NoFactoryAvailableException;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.k.Constants;
import org.apache.camel.k.ContextCustomizer;
import org.apache.camel.k.RoutesLoader;
import org.apache.camel.k.Source;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class RuntimeSupport {
    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeSupport.class);

    private RuntimeSupport() {
    }

    // *********************************
    //
    // Helpers - Customizers
    //
    // *********************************

    public static List<ContextCustomizer> configureContextCustomizers(CamelContext context) {
        List<ContextCustomizer> appliedCustomizers = new ArrayList<>();
        Map<String, ContextCustomizer> customizers = lookupCustomizers(context);

        customizers.entrySet().stream()
            .sorted(Map.Entry.comparingByValue())
            .forEach(e -> {
                LOGGER.info("Apply ContextCustomizer with id={} and type={}", e.getKey(), e.getValue().getClass().getName());

                PropertiesSupport.bindProperties(context, e.getValue(), "customizer." + e.getKey() + ".");
                e.getValue().apply(context);

                appliedCustomizers.add(e.getValue());
            });

        return appliedCustomizers;
    }

    public static Map<String, ContextCustomizer> lookupCustomizers(CamelContext context) {
        Map<String, ContextCustomizer> customizers = new ConcurrentHashMap<>();

        PropertiesComponent component = context.getComponent("properties", PropertiesComponent.class);
        Properties properties = component.loadProperties();

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
                });
        }

        return customizers;
    }

    public static ContextCustomizer lookupCustomizerByID(CamelContext context, String customizerId) {
        ContextCustomizer customizer = context.getRegistry().lookupByNameAndType(customizerId, ContextCustomizer.class);
        if (customizer == null) {
            try {
                customizer = context.adapt(ExtendedCamelContext.class)
                    .getFactoryFinder(Constants.CONTEXT_CUSTOMIZER_RESOURCE_PATH)
                    .newInstance(customizerId, ContextCustomizer.class)
                    .orElseThrow(() -> new RuntimeException("Error creating instance for customizer: " + customizerId));

                LOGGER.info("Found customizer {} with id {} rom service definition", customizer, customizerId);
            } catch (NoFactoryAvailableException e) {
                throw new RuntimeException(e);
            }
        } else {
            LOGGER.info("Found customizer {} with id {} from the registry", customizer, customizerId);
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
        return source.getLoader().map(
            loaderId -> lookupLoaderById(context, loaderId)
        ).orElseGet(
            () -> lookupLoaderByLanguage(context, source.getLanguage())
        );
    }


    public static RoutesLoader lookupLoaderById(CamelContext context, String loaderId) {
        LOGGER.info("Looking up loader for id: {}", loaderId);

        RoutesLoader loader = context.getRegistry().findByTypeWithName(RoutesLoader.class).get(loaderId);
        if (loader != null) {
            LOGGER.info("Found loader {} with id {} from the registry", loader, loaderId);
            return loader;
        }

        return lookupLoaderFromResource(context, loaderId);
    }

    public static RoutesLoader lookupLoaderByLanguage(CamelContext context, String loaderId) {
        LOGGER.info("Looking up loader for language: {}", loaderId);

        for (RoutesLoader loader: context.getRegistry().findByType(RoutesLoader.class)) {
            if (loader.getSupportedLanguages().contains(loaderId)) {
                LOGGER.info("Found loader {} for language {} from the registry", loader, loaderId);
                return loader;
            }
        }

        return lookupLoaderFromResource(context, loaderId);
    }

    public static RoutesLoader lookupLoaderFromResource(CamelContext context, String loaderId) {
        RoutesLoader loader;

        try {
            loader = context.adapt(ExtendedCamelContext.class)
                .getFactoryFinder(Constants.ROUTES_LOADER_RESOURCE_PATH)
                .newInstance(loaderId, RoutesLoader.class)
                .orElseThrow(() -> new RuntimeException("Error creating instance of loader: " + loaderId));

            LOGGER.info("Found loader {} for language {} from service definition", loader, loaderId);
        } catch (NoFactoryAvailableException e) {
            throw new IllegalArgumentException("Unable to find loader for: " + loaderId, e);
        }

        return loader;
    }
}

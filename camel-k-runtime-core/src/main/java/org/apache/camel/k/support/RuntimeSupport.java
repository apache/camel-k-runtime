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

import java.io.IOException;
import java.io.Reader;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.camel.CamelContext;
import org.apache.camel.NoFactoryAvailableException;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.k.Constants;
import org.apache.camel.k.ContextCustomizer;
import org.apache.camel.k.RoutesLoader;
import org.apache.camel.k.Runtime;
import org.apache.camel.k.Source;
import org.apache.camel.k.adapter.Introspection;
import org.apache.camel.spi.FactoryFinder;
import org.apache.camel.spi.RestConfiguration;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.StringHelper;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class RuntimeSupport {
    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeSupport.class);

    private RuntimeSupport() {
    }

    public static List<ContextCustomizer> configureContext(CamelContext context, Runtime.Registry registry) {
        List<ContextCustomizer> appliedCustomizers = new ArrayList<>();
        Set<String> customizers = lookupCustomizerIDs(context);

        // this is to initialize all customizers that might be already present in
        // the context injected by other means.
        for (Map.Entry<String, ContextCustomizer> entry: context.getRegistry().findByTypeWithName(ContextCustomizer.class).entrySet()) {
            if (!customizers.remove(entry.getKey())) {
                continue;
            }

            applyCustomizer(context, entry.getKey(), entry.getValue(), registry);

            appliedCustomizers.add(entry.getValue());
        }

        try {
            FactoryFinder finder = context.getFactoryFinder(Constants.CONTEXT_CUSTOMIZER_RESOURCE_PATH);

            for (String customizerId : customizers) {
                ContextCustomizer customizer = (ContextCustomizer) finder.newInstance(customizerId);
                applyCustomizer(context, customizerId, customizer, registry);

                appliedCustomizers.add(customizer);
            }
        } catch (NoFactoryAvailableException e) {
            throw new RuntimeException(e);
        }

        return appliedCustomizers;
    }

    public static void applyCustomizer(CamelContext context, String customizerId, ContextCustomizer customizer, Runtime.Registry registry) {
        ObjectHelper.notNull(customizer, "customizer");
        StringHelper.notEmpty(customizerId, "customizerId");

        LOGGER.info("Apply ContextCustomizer with id={} and type={}", customizerId, customizer.getClass().getName());

        bindProperties(context, customizer, "customizer." + customizerId + ".");
        customizer.apply(context, registry);
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

    public static void configureRest(CamelContext context) {
        RestConfiguration configuration = new RestConfiguration();

        if (RuntimeSupport.bindProperties(context, configuration, "camel.rest.") > 0) {
            //
            // Set the rest configuration if only if at least one
            // rest parameter has been set.
            //
            context.setRestConfiguration(configuration);
        }
    }

    public static String resolvePropertyPlaceholders(CamelContext context, String text) throws Exception {
        return context.resolvePropertyPlaceholders(
            context.getPropertyPrefixToken() + text + context.getPropertySuffixToken()
        );
    }

    public static int bindProperties(CamelContext context, Object target, String prefix) {
        final PropertiesComponent component = context.getComponent("properties", PropertiesComponent.class);
        final Properties properties = component.getInitialProperties();

        if (properties == null) {
            return 0;
        }

        return bindProperties(properties, target, prefix);
    }

    public static int bindProperties(Properties properties, Object target, String prefix) {
        final AtomicInteger count = new AtomicInteger();

        properties.entrySet().stream()
            .filter(entry -> entry.getKey() instanceof String)
            .filter(entry -> entry.getValue() != null)
            .filter(entry -> ((String)entry.getKey()).startsWith(prefix))
            .forEach(entry -> {
                    final String key = ((String)entry.getKey()).substring(prefix.length());
                    final Object val = entry.getValue();

                    try {
                        if (Introspection.setProperty(target, key, val)) {
                            count.incrementAndGet();
                        }
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }
            );

        return count.get();
    }

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

    public static Properties loadProperties() {
        final String conf = System.getProperty(Constants.PROPERTY_CAMEL_K_CONF, System.getenv(Constants.ENV_CAMEL_K_CONF));
        final String confd = System.getProperty(Constants.PROPERTY_CAMEL_K_CONF_D, System.getenv(Constants.ENV_CAMEL_K_CONF_D));

        return loadProperties(conf, confd);
    }

    public static Properties loadProperties(String conf, String confd) {
        final Properties properties = new Properties();

        // Main location
        if (ObjectHelper.isNotEmpty(conf)) {
            if (conf.startsWith(Constants.SCHEME_ENV)) {
                try (Reader reader = URIResolver.resolveEnv(conf)) {
                    properties.load(reader);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                try (Reader reader = Files.newBufferedReader(Paths.get(conf))) {
                    properties.load(reader);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        // Additional locations
        if (ObjectHelper.isNotEmpty(confd)) {
            Path root = Paths.get(confd);
            FileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Objects.requireNonNull(file);
                    Objects.requireNonNull(attrs);

                    String path = file.toFile().getAbsolutePath();
                    String ext = FilenameUtils.getExtension(path);

                    if (Objects.equals("properties", ext)) {
                        try (Reader reader = Files.newBufferedReader(Paths.get(path))) {
                            properties.load(reader);
                        }
                    }

                    return FileVisitResult.CONTINUE;
                }
            };

            if (Files.exists(root)) {
                try {
                    Files.walkFileTree(root, visitor);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return properties;
    }
}

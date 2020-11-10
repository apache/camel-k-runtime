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

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.camel.CamelContext;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.k.ContextCustomizer;
import org.apache.camel.k.Source;
import org.apache.camel.k.SourceLoader;
import org.apache.camel.spi.HasCamelContext;
import org.apache.camel.util.IOHelper;
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

    public static List<ContextCustomizer> configureContextCustomizers(HasCamelContext hasCamelContext) {
        return configureContextCustomizers(hasCamelContext.getCamelContext());
    }

    public static List<ContextCustomizer> configureContextCustomizers(CamelContext context) {
        List<ContextCustomizer> appliedCustomizers = new ArrayList<>();
        Map<String, ContextCustomizer> customizers = lookupCustomizers(context);

        customizers.entrySet().stream()
            .sorted(Map.Entry.comparingByValue())
            .forEach(e -> {
                LOGGER.debug("Apply ContextCustomizer with id={} and type={}", e.getKey(), e.getValue().getClass().getName());

                PropertiesSupport.bindProperties(context, e.getValue(), Constants.CUSTOMIZER_PREFIX + e.getKey() + ".");
                PropertiesSupport.bindProperties(context, e.getValue(), Constants.CUSTOMIZER_PREFIX_FALLBACK + e.getKey() + ".");

                e.getValue().apply(context);

                appliedCustomizers.add(e.getValue());
            });

        return appliedCustomizers;
    }

    public static Map<String, ContextCustomizer> lookupCustomizers(CamelContext context) {
        Map<String, ContextCustomizer> customizers = new ConcurrentHashMap<>();
        Properties properties = context.getPropertiesComponent().loadProperties(n -> n.startsWith(Constants.CUSTOMIZER_PREFIX) || n.startsWith(Constants.CUSTOMIZER_PREFIX_FALLBACK));

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

                    if (matcher.matches()) {
                        String customizerId = null;

                        if (matcher.groupCount() == 1) {
                            customizerId = matcher.group(1);
                        } else if (matcher.groupCount() == 2) {
                            customizerId = matcher.group(2);
                        }

                        if (customizerId != null && Boolean.parseBoolean(String.valueOf(val))) {
                            //
                            // Do not override customizers eventually found
                            // in the registry
                            //
                            customizers.computeIfAbsent(customizerId, id -> lookupCustomizerByID(context, id));
                        }
                    }
                });
        }

        return customizers;
    }

    public static ContextCustomizer lookupCustomizerByID(CamelContext context, String customizerId) {
        ContextCustomizer customizer = context.getRegistry().lookupByNameAndType(customizerId, ContextCustomizer.class);
        if (customizer == null) {
            customizer = context.adapt(ExtendedCamelContext.class)
                .getFactoryFinder(Constants.CONTEXT_CUSTOMIZER_RESOURCE_PATH)
                .newInstance(customizerId, ContextCustomizer.class)
                .orElseThrow(() -> new RuntimeException("Error creating instance for customizer: " + customizerId));

            LOGGER.debug("Found customizer {} with id {} from service definition", customizer, customizerId);
        } else {
            LOGGER.debug("Found customizer {} with id {} from the registry", customizer, customizerId);
        }

        return customizer;
    }

    public static Set<String> lookupCustomizerIDs(CamelContext context) {
        Set<String> customizers = new TreeSet<>();

        String customizerIDs = System.getenv().getOrDefault(Constants.ENV_CAMEL_K_CUSTOMIZERS, "");
        if (ObjectHelper.isEmpty(customizerIDs)) {
            // TODO: getPropertiesComponent().resolveProperty() throws exception instead
            //       of returning abd empty optional
            customizerIDs = context.getPropertiesComponent()
                .loadProperties(Constants.PROPERTY_CAMEL_K_CUSTOMIZER::equals)
                .getProperty(Constants.PROPERTY_CAMEL_K_CUSTOMIZER, "");
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

    public static SourceLoader loaderFor(CamelContext context, Source source) {
        return source.getLoader().map(
            loaderId -> lookupLoaderById(context, loaderId)
        ).orElseGet(
            () -> lookupLoaderByLanguage(context, source.getLanguage())
        );
    }


    public static SourceLoader lookupLoaderById(CamelContext context, String loaderId) {
        LOGGER.debug("Looking up loader for id: {}", loaderId);

        SourceLoader loader = context.getRegistry().findByTypeWithName(SourceLoader.class).get(loaderId);
        if (loader != null) {
            LOGGER.debug("Found loader {} with id {} from the registry", loader, loaderId);
            return loader;
        }

        return lookupLoaderFromResource(context, loaderId);
    }

    public static SourceLoader lookupLoaderByLanguage(CamelContext context, String loaderId) {
        LOGGER.debug("Looking up loader for language: {}", loaderId);

        for (SourceLoader loader: context.getRegistry().findByType(SourceLoader.class)) {
            if (loader.getSupportedLanguages().contains(loaderId)) {
                LOGGER.debug("Found loader {} for language {} from the registry", loader, loaderId);
                return loader;
            }
        }

        return lookupLoaderFromResource(context, loaderId);
    }

    public static SourceLoader lookupLoaderFromResource(CamelContext context, String loaderId) {
        SourceLoader loader = context.adapt(ExtendedCamelContext.class)
            .getFactoryFinder(Constants.SOURCE_LOADER_RESOURCE_PATH)
            .newInstance(loaderId, SourceLoader.class)
            .orElseThrow(() -> new RuntimeException("Error creating instance of loader: " + loaderId));

        LOGGER.debug("Found loader {} for language {} from service definition", loader, loaderId);

        return loader;
    }

    // *********************************
    //
    // Helpers - Interceptors
    //
    // *********************************

    public static List<SourceLoader.Interceptor> loadInterceptors(CamelContext context, Source source) {
        ExtendedCamelContext ecc = context.adapt(ExtendedCamelContext.class);
        List<SourceLoader.Interceptor> answer = new ArrayList<>();

        for (String id : source.getInterceptors()) {
            try {
                // first check the registry
                SourceLoader.Interceptor interceptor = ecc.getRegistry()
                    .lookupByNameAndType(id, SourceLoader.Interceptor.class);

                if (interceptor == null) {
                    // then try with factory finder
                    interceptor = ecc.getFactoryFinder(Constants.SOURCE_LOADER_INTERCEPTOR_RESOURCE_PATH)
                        .newInstance(id, SourceLoader.Interceptor.class)
                        .orElseThrow(() -> new IllegalArgumentException("Unable to find source loader interceptor for: " + id));

                    LOGGER.debug("Found source loader interceptor {} from service definition", id);
                } else {
                    LOGGER.debug("Found source loader interceptor {} from registry", id);
                }

                PropertiesSupport.bindProperties(context, interceptor, Constants.LOADER_INTERCEPTOR_PREFIX + id + ".");
                PropertiesSupport.bindProperties(context, interceptor, Constants.LOADER_INTERCEPTOR_PREFIX_FALLBACK + id + ".");

                answer.add(interceptor);
            } catch (Exception e) {
                throw new IllegalArgumentException("Unable to find source loader interceptor for: " + id, e);
            }
        }

        return answer;
    }

    // *********************************
    //
    // Helpers - Misc
    //
    // *********************************

    public static String getRuntimeVersion() {
        String version = null;

        InputStream is = null;
        // try to load from maven properties first
        try {
            Properties p = new Properties();
            is = RuntimeSupport.class.getResourceAsStream("/META-INF/maven/org.apache.camel.k/camel-k-runtime-core/pom.properties");
            if (is != null) {
                p.load(is);
                version = p.getProperty("version", "");
            }
        } catch (Exception e) {
            // ignore
        } finally {
            if (is != null) {
                IOHelper.close(is);
            }
        }

        // fallback to using Java API
        if (version == null) {
            Package aPackage = RuntimeSupport.class.getPackage();
            if (aPackage != null) {
                version = aPackage.getImplementationVersion();
                if (version == null) {
                    version = aPackage.getSpecificationVersion();
                }
            }
        }

        if (version == null) {
            // we could not compute the version so use a blank
            version = "";
        }

        return Objects.requireNonNull(version, "Could not determine Camel K Runtime version");
    }

    // *********************************
    //
    // Properties
    //
    // *********************************

    public static Map<String, String> loadApplicationProperties() {
        final String conf = System.getProperty(Constants.PROPERTY_CAMEL_K_CONF, System.getenv(Constants.ENV_CAMEL_K_CONF));
        final Map<String, String> properties = new HashMap<>();

        if (ObjectHelper.isEmpty(conf)) {
            return properties;
        }

        try {
            Path confPath = Paths.get(conf);

            if (Files.exists(confPath)) {
                try (Reader reader = Files.newBufferedReader(confPath)) {
                    Properties p = new Properties();
                    p.load(reader);
                    p.forEach((key, value) -> properties.put(String.valueOf(key), String.valueOf(value)));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return properties;
    }

    public static Map<String, String> loadUserProperties() {
        final String conf = System.getProperty(Constants.PROPERTY_CAMEL_K_CONF_D, System.getenv(Constants.ENV_CAMEL_K_CONF_D));
        final Map<String, String> properties = new HashMap<>();

        if (ObjectHelper.isEmpty(conf)) {
            return properties;
        }

        FileVisitor<Path> visitor = new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Objects.requireNonNull(file);
                Objects.requireNonNull(attrs);

                if (file.toFile().getAbsolutePath().endsWith(".properties")) {
                    try (Reader reader = Files.newBufferedReader(file)) {
                        Properties p = new Properties();
                        p.load(reader);
                        p.forEach((key, value) -> properties.put(String.valueOf(key), String.valueOf(value)));
                    }
                } else {
                    properties.put(
                        file.getFileName().toString(),
                        Files.readString(file, StandardCharsets.UTF_8));
                }

                return FileVisitResult.CONTINUE;
            }
        };

        Path root = Paths.get(conf);

        if (Files.exists(root)) {
            try {
                Files.walkFileTree(root, visitor);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return properties;
    }
}

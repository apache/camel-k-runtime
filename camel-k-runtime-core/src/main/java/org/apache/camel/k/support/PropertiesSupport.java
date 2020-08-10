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
import java.io.Reader;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.k.Constants;
import org.apache.camel.spi.PropertiesComponent;
import org.apache.camel.spi.PropertyConfigurer;
import org.apache.camel.support.PropertyBindingSupport;
import org.apache.camel.support.service.ServiceHelper;
import org.apache.camel.util.ObjectHelper;

public final class PropertiesSupport {
    private PropertiesSupport() {
    }

    public static <T> T bindProperties(CamelContext context, T target, String prefix) {
        return bindProperties(context, target, prefix, false);
    }

    public static <T> T bindProperties(CamelContext context, T target, String prefix, boolean stripPrefix) {
        return bindProperties(context, target, k -> k.startsWith(prefix), prefix, stripPrefix);
    }

    public static <T> T bindProperties(CamelContext context, T target, Predicate<String> filter, String prefix) {
        return bindProperties(context, target, filter, prefix, false);
    }

    public static <T> T bindProperties(CamelContext context, T target, Predicate<String> filter, String prefix, boolean stripPrefix) {
        final PropertiesComponent component = context.getPropertiesComponent();
        final Properties propertiesWithPrefix = component.loadProperties(filter);
        final Map<String, Object> properties = new HashMap<>();

        propertiesWithPrefix.stringPropertyNames().forEach(
            name -> properties.put(
                stripPrefix ? name.substring(prefix.length()) : name,
                propertiesWithPrefix.getProperty(name))
        );

        PropertyConfigurer configurer = null;
        if (target instanceof Component) {
            // the component needs to be initialized to have the configurer ready
            ServiceHelper.initService(target);
            configurer = ((Component) target).getComponentPropertyConfigurer();
        }

        if (configurer == null) {
            String name = target.getClass().getName();
            if (target instanceof ExtendedCamelContext) {
                // special for camel context itself as we have an extended configurer
                name = ExtendedCamelContext.class.getName();
            }

            // see if there is a configurer for it
            configurer = context.adapt(ExtendedCamelContext.class)
                .getConfigurerResolver()
                .resolvePropertyConfigurer(name, context);
        }

        PropertyBindingSupport.build()
            .withIgnoreCase(true)
            .withCamelContext(context)
            .withTarget(target)
            .withProperties(properties)
            .withRemoveParameters(true)
            .withOptionPrefix(stripPrefix ? null : prefix)
            .withConfigurer(configurer)
            .bind();

        return target;
    }

    public static String resolveApplicationPropertiesLocation() {
        return System.getProperty(Constants.PROPERTY_CAMEL_K_CONF, System.getenv(Constants.ENV_CAMEL_K_CONF));
    }

    public static Properties loadApplicationProperties() {
        final String conf = resolveApplicationPropertiesLocation();
        final Properties properties = new Properties();

        if (ObjectHelper.isEmpty(conf)) {
            return properties;
        }

        try {
            Path confPath = Paths.get(conf);

            if (Files.exists(confPath)) {
                try (Reader reader = Files.newBufferedReader(confPath)) {
                    properties.load(reader);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return properties;
    }

    public static String resolveUserPropertiesLocation() {
        return System.getProperty(Constants.PROPERTY_CAMEL_K_CONF_D, System.getenv(Constants.ENV_CAMEL_K_CONF_D));
    }

    public static Properties loadUserProperties() {
        final Properties properties = new Properties();

        try {
            for (String location: resolveUserPropertiesLocations()) {
                try (Reader reader = Files.newBufferedReader(Paths.get(location))) {
                    properties.load(reader);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return properties;
    }

    public static Properties loadProperties() {
        final Properties app = loadApplicationProperties();
        final Properties usr = loadUserProperties();

        app.putAll(usr);

        return app;
    }

    public static Collection<String> resolveUserPropertiesLocations() {
        final String conf = resolveUserPropertiesLocation();
        final Set<String> locations = new LinkedHashSet<>();

        // Additional locations
        if (ObjectHelper.isNotEmpty(conf)) {
            Path root = Paths.get(conf);
            FileVisitor<Path> visitor = new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Objects.requireNonNull(file);
                    Objects.requireNonNull(attrs);

                    final String path = file.toFile().getAbsolutePath();
                    if (path.endsWith(".properties")) {
                        locations.add(path);
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

        return locations;
    }
}

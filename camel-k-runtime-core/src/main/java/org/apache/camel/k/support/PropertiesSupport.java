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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import org.apache.camel.CamelContext;
import org.apache.camel.k.Constants;
import org.apache.camel.spi.PropertiesComponent;
import org.apache.camel.support.PropertyBindingSupport;
import org.apache.camel.util.ObjectHelper;

public final class PropertiesSupport {
    private PropertiesSupport() {
    }

    @SuppressWarnings("unchecked")
    public static boolean bindProperties(CamelContext context, Object target, String prefix) {
        final PropertiesComponent component = context.getPropertiesComponent();
        final Properties properties = component.loadProperties(k -> k.startsWith(prefix));

        return PropertyBindingSupport.build()
            .withCamelContext(context)
            .withTarget(target)
            .withProperties((Map)properties)
            .withRemoveParameters(false)
            .withOptionPrefix(prefix)
            .bind();
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
            FileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {
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

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
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.k.Constants;
import org.apache.camel.support.PropertyBindingSupport;
import org.apache.camel.util.ObjectHelper;
import org.apache.commons.io.FilenameUtils;

public final class PropertiesSupport {
    private PropertiesSupport() {
    }

    @SuppressWarnings("unchecked")
    public static boolean bindProperties(CamelContext context, Object target, String prefix) {
        final PropertiesComponent component = context.getComponent("properties", PropertiesComponent.class);
        final Properties properties = component.loadProperties(k -> k.startsWith(prefix));

        return PropertyBindingSupport.build()
            .withCamelContext(context)
            .withTarget(target)
            .withProperties((Map)properties)
            .withRemoveParameters(false)
            .withOptionPrefix(prefix)
            .bind();
    }

    public static Properties loadProperties() {
        return loadProperties(
            System.getProperty(Constants.PROPERTY_CAMEL_K_CONF, System.getenv(Constants.ENV_CAMEL_K_CONF)),
            System.getProperty(Constants.PROPERTY_CAMEL_K_CONF_D, System.getenv(Constants.ENV_CAMEL_K_CONF_D))
        );
    }

    public static Properties loadProperties(String conf, String confd) {
        final Properties properties = new Properties();
        final Collection<String> locations = resolvePropertiesLocation(conf, confd);

        try {
            for (String location: locations) {
                try (Reader reader = Files.newBufferedReader(Paths.get(location))) {
                    properties.load(reader);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return properties;
    }

    public static Collection<String> resolvePropertiesLocation(String conf, String confd) {
        final Set<String> locations = new LinkedHashSet<>();

        // Main location
        if (ObjectHelper.isNotEmpty(conf)) {
            locations.add(conf);
        }

        // Additional locations
        if (ObjectHelper.isNotEmpty(confd)) {
            Path root = Paths.get(confd);
            FileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Objects.requireNonNull(file);
                    Objects.requireNonNull(attrs);

                    final String path = file.toFile().getAbsolutePath();
                    final String ext = FilenameUtils.getExtension(path);

                    if (Objects.equals("properties", ext)) {
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

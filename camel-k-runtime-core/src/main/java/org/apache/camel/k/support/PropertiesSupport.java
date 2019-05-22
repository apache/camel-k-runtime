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
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import org.apache.camel.CamelContext;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.k.Constants;
import org.apache.camel.support.IntrospectionSupport;
import org.apache.camel.util.ObjectHelper;
import org.apache.commons.io.FilenameUtils;

public final class PropertiesSupport {
    private PropertiesSupport() {
    }

    public static void forEachProperty(CamelContext context, Predicate<String> nameFilter, BiConsumer<String, Object> consumer) {
        final PropertiesComponent component = context.getComponent("properties", PropertiesComponent.class);
        final Properties properties = component.getInitialProperties();

        if (properties != null) {
            for (String name: properties.stringPropertyNames()) {
                if (nameFilter.test(name)) {
                    consumer.accept(name,  properties.get(name));
                }
            }
        }
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
                        if (IntrospectionSupport.setProperty(target, key, val)) {
                            count.incrementAndGet();
                        }
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }
            );

        return count.get();
    }

    public static Properties loadProperties() {
        return loadProperties(
            System.getProperty(Constants.PROPERTY_CAMEL_K_CONF, System.getenv(Constants.ENV_CAMEL_K_CONF)),
            System.getProperty(Constants.PROPERTY_CAMEL_K_CONF_D, System.getenv(Constants.ENV_CAMEL_K_CONF_D))
        );
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

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
package org.apache.camel.k.quarkus;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import io.smallrye.config.PropertiesConfigSource;
import org.apache.camel.k.Constants;
import org.apache.camel.k.support.PropertiesSupport;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationRuntimeConfigSourceProvider implements ConfigSourceProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationRuntimeConfigSourceProvider.class);

    @Override
    public Iterable<ConfigSource> getConfigSources(ClassLoader forClassLoader) {
        final String conf = System.getProperty(Constants.PROPERTY_CAMEL_K_CONF, System.getenv(Constants.ENV_CAMEL_K_CONF));
        final String confd = System.getProperty(Constants.PROPERTY_CAMEL_K_CONF_D, System.getenv(Constants.ENV_CAMEL_K_CONF_D));
        final List<ConfigSource> sources = new ArrayList<>();

        try {
            for (String location : PropertiesSupport.resolvePropertiesLocations(conf, confd)) {
                LOGGER.info("Register properties location: {}", location);

                sources.add(
                    new PropertiesConfigSource(Paths.get(location).toUri().toURL())
                );
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return sources;
    }
}

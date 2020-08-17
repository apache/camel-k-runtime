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

import java.util.Collections;
import java.util.Properties;

import io.smallrye.config.PropertiesConfigSource;
import org.apache.camel.k.support.PropertiesSupport;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

public class ApplicationRuntimeConfigSourceProvider implements ConfigSourceProvider {
    @Override
    public Iterable<ConfigSource> getConfigSources(ClassLoader forClassLoader) {
        final Properties applicationProperties = PropertiesSupport.loadProperties();
        final Properties quarkusProperties = new Properties();

        for (String name : applicationProperties.stringPropertyNames()) {
            if (name.startsWith("quarkus.")) {
                quarkusProperties.put(name, applicationProperties.get(name));
            }
        }

        return Collections.singletonList(
            new PropertiesConfigSource(quarkusProperties, "camel-k")
        );
    }
}

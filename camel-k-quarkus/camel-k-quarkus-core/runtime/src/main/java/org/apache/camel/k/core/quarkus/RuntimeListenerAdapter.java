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
package org.apache.camel.k.core.quarkus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.k.Runtime;
import org.apache.camel.main.BaseMainSupport;
import org.apache.camel.main.MainListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RuntimeListenerAdapter implements MainListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeListenerAdapter.class);

    private final List<Runtime.Listener> listeners;

    public RuntimeListenerAdapter() {
        this.listeners = new ArrayList<>();
    }

    public void setListeners(List<Runtime.Listener> listeners) {
        this.listeners.clear();
        this.listeners.addAll(listeners);
    }

    @Override
    public void beforeStart(BaseMainSupport main) {
        invokeListeners(listeners, on(main), Runtime.Phase.Starting);
    }

    @Override
    public void beforeConfigure(BaseMainSupport main) {
        invokeListeners(listeners, on(main), Runtime.Phase.ConfigureRoutes);
    }

    @Override
    public void configure(CamelContext context) {
        invokeListeners(listeners, on(context), Runtime.Phase.ConfigureContext);
    }

    @Override
    public void afterStart(BaseMainSupport main) {
        invokeListeners(listeners, on(main), Runtime.Phase.Started);
    }

    @Override
    public void beforeStop(BaseMainSupport main) {
        invokeListeners(listeners, on(main), Runtime.Phase.Stopping);
    }

    @Override
    public void afterStop(BaseMainSupport main) {
        invokeListeners(listeners, on(main), Runtime.Phase.Stopped);
    }

    // ************************
    //
    // Helpers
    //
    // ************************

    private static void invokeListeners(List<Runtime.Listener> listeners, Runtime runtime, Runtime.Phase phase) {
        listeners.stream()
            .sorted(Comparator.comparingInt(Runtime.Listener::getOrder))
            .forEach(l -> {
                if (l.accept(phase, runtime)) {
                    LOGGER.info("Listener {} executed in phase {}", l, phase);
                }
            });
    }

    private static Runtime on(CamelContext context) {
        return Runtime.on(context);
    }

    private static Runtime on(BaseMainSupport main) {
        return new Runtime() {
            @Override
            public CamelContext getCamelContext() {
                return main.getCamelContext();
            }

            @Override
            public void addRoutes(RoutesBuilder builder) {
                main.addRoutesBuilder(builder);
            }

            @Override
            public void addConfiguration(Object configuration) {
                main.addConfiguration(configuration);
            }

            @Override
            public void setInitialProperties(Properties properties) {
                main.setInitialProperties(properties);
            }

            @Override
            public void setProperties(Properties properties) {
                main.setOverrideProperties(properties);
            }

            @Override
            public void setPropertiesLocations(Collection<String> locations) {
                main.setPropertyPlaceholderLocations(
                    locations.stream()
                        .map(location -> location.startsWith("file:") ? location : "file:" + location)
                        .distinct()
                        .sorted()
                        .collect(Collectors.joining(","))
                );
            }
        };
    }
}

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
package org.apache.camel.k.main;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.k.CompositeClassloader;
import org.apache.camel.k.Runtime;
import org.apache.camel.k.support.PropertiesSupport;
import org.apache.camel.main.BaseMainSupport;
import org.apache.camel.main.MainSupport;
import org.apache.camel.main.RoutesCollector;
import org.apache.camel.model.RoutesDefinition;
import org.apache.camel.model.rest.RestsDefinition;
import org.apache.camel.spi.HasId;
import org.apache.camel.util.function.ThrowingConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ApplicationRuntime implements Runtime {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationRuntime.class);

    private final MainSupport main;
    private final DefaultCamelContext context;
    private final Set<Runtime.Listener> listeners;

    public ApplicationRuntime() {
        this.listeners = new LinkedHashSet<>();

        this.context = new DefaultCamelContext();
        this.context.setName("camel-k");
        this.context.setApplicationContextClassLoader(new CompositeClassloader());

        this.main = new MainAdapter();
        this.main.configure().setXmlRoutes("false");
        this.main.configure().setXmlRests("false");
        this.main.setDefaultPropertyPlaceholderLocation("false");
        this.main.setRoutesCollector(new NoRoutesCollector());
        this.main.addMainListener(new MainListenerAdapter());
    }

    @Override
    public CamelContext getCamelContext() {
        return this.context;
    }

    public void run() throws Exception {
        this.main.run();
    }

    @Override
    public void stop() throws Exception {
        this.main.stop();
    }

    @Override
    public void addRoutes(RoutesBuilder builder) {
        this.main.configure().addRoutesBuilder(builder);
    }

    @Override
    public void addConfiguration(Object configuration) {
        this.main.configure().addConfiguration(configuration);
    }

    @Override
    public void setInitialProperties(Properties properties) {
        this.main.getCamelContext().getPropertiesComponent().setInitialProperties(properties);
    }

    @Override
    public void setProperties(Properties properties) {
        this.main.getCamelContext().getPropertiesComponent().setOverrideProperties(properties);
    }

    public void addListeners(Iterable<Runtime.Listener> listeners) {
        listeners.forEach(this::addListener);
    }

    public void addListener(Runtime.Listener listener) {
        if (listener instanceof HasId) {
            String id = ((HasId) listener).getId();
            if (!id.endsWith(".")) {
                id = id + ".";
            }

            PropertiesSupport.bindProperties(getCamelContext(), listener, id);
        }

        LOGGER.info("Add listener: {}", listener);

        this.listeners.add(listener);
    }

    public void addListener(Phase phase, ThrowingConsumer<Runtime, Exception> consumer) {
        addListener((p, runtime) -> {
            if (p == phase) {
                try {
                    consumer.accept(runtime);
                    return true;
                } catch (Exception e) {
                    throw RuntimeCamelException.wrapRuntimeCamelException(e);
                }
            }

            return false;
        });
    }

    private final class MainListenerAdapter implements org.apache.camel.main.MainListener {
        @Override
        public void beforeInitialize(BaseMainSupport main) {
            invokeListeners(Phase.ConfigureProperties);
        }

        @Override
        public void beforeConfigure(BaseMainSupport main) {
            invokeListeners(Phase.ConfigureRoutes);
        }

        @Override
        public void afterConfigure(BaseMainSupport main) {
            invokeListeners(Phase.ConfigureContext);
        }

        @Override
        public void configure(CamelContext context) {
        }

        @Override
        public void beforeStart(BaseMainSupport main) {
            invokeListeners(Phase.Starting);
        }

        @Override
        public void afterStart(BaseMainSupport main) {
            invokeListeners(Phase.Started);
        }

        @Override
        public void beforeStop(BaseMainSupport main) {
            invokeListeners(Phase.Stopping);
        }

        @Override
        public void afterStop(BaseMainSupport main) {
            invokeListeners(Phase.Stopped);
        }

        private void invokeListeners(Phase phase) {
            listeners.stream()
                .sorted(Comparator.comparingInt(Listener::getOrder))
                .forEach(l -> {
                    if (l.accept(phase, ApplicationRuntime.this)) {
                        LOGGER.info("Listener {} executed in phase {}", l, phase);
                    }
                });
        }
    }

    private final class MainAdapter extends MainSupport {
        @Override
        public CamelContext getCamelContext() {
            return ApplicationRuntime.this.context;
        }

        @Override
        protected CamelContext createCamelContext() {
            return ApplicationRuntime.this.context;
        }

        @Override
        protected void doInit() throws Exception {
            super.doInit();
            initCamelContext();
        }

        @Override
        protected void doStart() throws Exception {
            super.doStart();
            if (getCamelContext() != null) {
                try {
                    // if we were veto started then mark as completed
                    getCamelContext().start();
                } finally {
                    if (getCamelContext().isVetoStarted()) {
                        completed();
                    }
                }
            }
        }

        @Override
        protected void doStop() throws Exception {
            super.doStop();
            if (getCamelContext() != null) {
                getCamelContext().stop();
            }
        }

        @Override
        protected ProducerTemplate findOrCreateCamelTemplate() {
            if (getCamelContext() != null) {
                return getCamelContext().createProducerTemplate();
            } else {
                return null;
            }
        }
    }

    private static final class NoRoutesCollector implements RoutesCollector {
        @Override
        public List<RoutesBuilder> collectRoutesFromRegistry(CamelContext camelContext, String excludePattern, String includePattern) {
            return Collections.emptyList();
        }

        @Override
        public List<RoutesDefinition> collectXmlRoutesFromDirectory(CamelContext camelContext, String directory) throws Exception {
            return Collections.emptyList();
        }

        @Override
        public List<RestsDefinition> collectXmlRestsFromDirectory(CamelContext camelContext, String directory) throws Exception {
            return Collections.emptyList();
        }
    }
}


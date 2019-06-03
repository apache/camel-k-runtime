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
package org.apache.camel.k.jvm;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.k.Runtime;
import org.apache.camel.k.support.PropertiesSupport;
import org.apache.camel.main.Main;
import org.apache.camel.main.MainSupport;
import org.apache.camel.spi.HasId;
import org.apache.camel.util.function.ThrowingConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ApplicationRuntime implements Runtime {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationRuntime.class);

    private final Main main;
    private final DefaultCamelContext context;
    private final Set<Runtime.Listener> listeners;

    public ApplicationRuntime() {
        this.listeners = new LinkedHashSet<>();

        this.context = new DefaultCamelContext();
        this.context.setName("camel-k");

        this.main = new Main() {
            @Override
            protected ProducerTemplate findOrCreateCamelTemplate() {
                return context.createProducerTemplate();
            }
            @Override
            protected CamelContext createCamelContext() {
                return context;
            }
        };

        this.main.addMainListener(new MainListenerAdapter());
    }

    @Override
    public CamelContext getCamelContext() {
        return context;
    }

    public void run() throws Exception {
        this.main.run();
    }

    public void stop()throws Exception {
        this.main.stop();
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

    private class MainListenerAdapter implements org.apache.camel.main.MainListener {
        @Override
        public void beforeStart(MainSupport main) {
            invokeListeners(Phase.Starting);
        }

        @Override
        public void configure(CamelContext context) {
            invokeListeners(Phase.ConfigureContext);
            invokeListeners(Phase.ConfigureRoutes);
        }

        @Override
        public void afterStart(MainSupport main) {
            invokeListeners(Phase.Started);
        }

        @Override
        public void beforeStop(MainSupport main) {
            invokeListeners(Phase.Stopping);
        }

        @Override
        public void afterStop(MainSupport main) {
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
}

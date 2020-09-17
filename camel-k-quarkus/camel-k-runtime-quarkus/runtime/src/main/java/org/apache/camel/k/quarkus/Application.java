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

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.runtime.Quarkus;
import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.main.BaseMainSupport;
import org.apache.camel.main.MainListener;

public final class Application {
    private Application() {
    }

    /**
     * The camel-k runtime impl based on camel-quarkus
     */
    public static class Runtime implements org.apache.camel.k.Runtime {
        private final BaseMainSupport main;
        private final AtomicBoolean stopped;

        public Runtime(BaseMainSupport main) {
            this.main = main;
            this.stopped = new AtomicBoolean();
        }

        @Override
        public CamelContext getCamelContext() {
            return main.getCamelContext();
        }

        @Override
        public void addRoutes(RoutesBuilder builder) {
            main.configure().addRoutesBuilder(builder);
        }

        @Override
        public void addConfiguration(Object configuration) {
            main.configure().addConfiguration(configuration);
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
        public void stop() throws Exception {
            if (!this.stopped.compareAndExchange(false, true)) {
                instance(ShutdownTask.class).ifPresentOrElse(
                    ShutdownTask::run,
                    Quarkus::asyncExit);
            }
        }
    }

    /**
     * Adapts main events to camel-k runtime lifecycle
     */
    public static class ListenerAdapter implements MainListener {
        private final org.apache.camel.k.Runtime.Listener[] listeners;

        public ListenerAdapter(List<org.apache.camel.k.Runtime.Listener> listeners) {
            this.listeners = listeners.stream()
                .sorted(Comparator.comparingInt(org.apache.camel.k.Runtime.Listener::getOrder))
                .toArray(org.apache.camel.k.Runtime.Listener[]::new);
        }

        @Override
        public void beforeInitialize(BaseMainSupport main) {
            invokeListeners(org.apache.camel.k.Runtime.Phase.ConfigureProperties);
        }

        @Override
        public void beforeConfigure(BaseMainSupport main) {
            invokeListeners(org.apache.camel.k.Runtime.Phase.ConfigureRoutes);
        }

        @Override
        public void afterConfigure(BaseMainSupport main) {
            invokeListeners(org.apache.camel.k.Runtime.Phase.ConfigureContext);
        }

        @Override
        public void configure(CamelContext context) {
            // no-op
        }

        @Override
        public void beforeStart(BaseMainSupport main) {
            invokeListeners(org.apache.camel.k.Runtime.Phase.Starting);
        }

        @Override
        public void afterStart(BaseMainSupport main) {
            invokeListeners(org.apache.camel.k.Runtime.Phase.Started);
        }

        @Override
        public void beforeStop(BaseMainSupport main) {
            invokeListeners(org.apache.camel.k.Runtime.Phase.Stopping);
        }

        @Override
        public void afterStop(BaseMainSupport main) {
            invokeListeners(org.apache.camel.k.Runtime.Phase.Stopped);
        }

        private void invokeListeners(org.apache.camel.k.Runtime.Phase phase) {
            org.apache.camel.k.Runtime runtime = instance(org.apache.camel.k.Runtime.class)
                .orElseThrow(() -> new IllegalStateException("Unable to fine a Runtime instance"));

            for (int i = 0; i < listeners.length; i ++) {
                listeners[i].accept(phase, runtime);
            }
        }
    }

    /**
     * Provide the task to be executed to shutdown the runtime
     */
    @FunctionalInterface
    public interface ShutdownTask {
        void run();
    }

    // *********************************
    //
    // Helpers
    //
    // *********************************

    public static Optional<ArcContainer> container() {
        return Optional.of(Arc.container());
    }

    public static <T> Optional<T> instance(Class<T> type) {
        return container()
            .map(container -> container.instance(type))
            .map(InstanceHandle::get);
    }
}

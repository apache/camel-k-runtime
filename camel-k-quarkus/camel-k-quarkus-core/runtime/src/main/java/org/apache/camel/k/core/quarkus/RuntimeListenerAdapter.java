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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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

    public List<Runtime.Listener> getListeners() {
        return Collections.unmodifiableList(listeners);
    }

    @Override
    public void beforeStart(BaseMainSupport main) {
        final Runtime runtime = new Runtime() {
            @Override
            public CamelContext getCamelContext() {
                return main.getCamelContext();
            }

            @Override
            public void addRoutes(RoutesBuilder builder) {
                try {
                    // TODO: the before start event is fired in the wrong
                    //       phase in camek-quarkus so routes have to be
                    //       added directly to the registry, eplace with:
                    //           main.addRoutesBuilder(builder)
                    //       when fixed.
                    main.getCamelContext().addRoutes(builder);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };

        invokeListeners(runtime, Runtime.Phase.Starting);
        invokeListeners(runtime, Runtime.Phase.ConfigureRoutes);
    }

    @Override
    public void configure(CamelContext context) {
        invokeListeners(Runtime.of(context), Runtime.Phase.ConfigureContext);
    }

    @Override
    public void afterStart(BaseMainSupport main) {
        final Runtime runtime = new Runtime() {
            @Override
            public CamelContext getCamelContext() {
                return main.getCamelContext();
            }

            @Override
            public void addRoutes(RoutesBuilder builder) {
                try {
                    // TODO: the before start event is fired in the wrong
                    //       phase in camek-quarkus so routes have to be
                    //       added directly to the registry, eplace with:
                    //           main.addRoutesBuilder(builder)
                    //       when fixed.
                    main.getCamelContext().addRoutes(builder);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };

        invokeListeners(runtime, Runtime.Phase.Started);
    }

    @Override
    public void beforeStop(BaseMainSupport main) {
        final Runtime runtime = new Runtime() {
            @Override
            public CamelContext getCamelContext() {
                return main.getCamelContext();
            }

            @Override
            public void addRoutes(RoutesBuilder builder) {
                try {
                    // TODO: the before start event is fired in the wrong
                    //       phase in camek-quarkus so routes have to be
                    //       added directly to the registry, eplace with:
                    //           main.addRoutesBuilder(builder)
                    //       when fixed.
                    main.getCamelContext().addRoutes(builder);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };

        invokeListeners(runtime, Runtime.Phase.Stopping);
    }

    @Override
    public void afterStop(BaseMainSupport main) {
        final Runtime runtime = new Runtime() {
            @Override
            public CamelContext getCamelContext() {
                return main.getCamelContext();
            }

            @Override
            public void addRoutes(RoutesBuilder builder) {
                try {
                    // TODO: the before start event is fired in the wrong
                    //       phase in camek-quarkus so routes have to be
                    //       added directly to the registry, eplace with:
                    //           main.addRoutesBuilder(builder)
                    //       when fixed.
                    main.getCamelContext().addRoutes(builder);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };

        invokeListeners(runtime, Runtime.Phase.Stopped);
    }

    private void invokeListeners(Runtime runtime, Runtime.Phase phase) {
        listeners.stream()
            .sorted(Comparator.comparingInt(Runtime.Listener::getOrder))
            .forEach(l -> {
                if (l.accept(phase, runtime)) {
                    LOGGER.info("Listener {} executed in phase {}", l, phase);
                }
            });
    }
}

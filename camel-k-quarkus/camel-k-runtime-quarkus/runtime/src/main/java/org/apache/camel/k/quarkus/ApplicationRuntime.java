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

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.quarkus.runtime.StartupEvent;
import org.apache.camel.CamelContext;
import org.apache.camel.k.Runtime;
import org.apache.camel.k.support.PropertiesSupport;
import org.apache.camel.quarkus.core.runtime.CamelRuntime;
import org.apache.camel.quarkus.core.runtime.StartedEvent;
import org.apache.camel.quarkus.core.runtime.StartingEvent;
import org.apache.camel.quarkus.core.runtime.StoppedEvent;
import org.apache.camel.quarkus.core.runtime.StoppingEvent;
import org.apache.camel.spi.HasId;
import org.apache.camel.spi.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ApplicationRuntime implements Runtime {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationRuntime.class);

    @Inject
    CamelRuntime runtime;

    private List<Listener> listeners = new ArrayList<>();
    
    public void startup(@Observes StartupEvent event) {
        LOGGER.info("Quarkus startup");
    }

    public void starting(@Observes StartingEvent event) {
        LOGGER.info("Camel starting");

        listeners.clear();

        //
        // Load and configure listeners
        //
        ServiceLoader.load(Runtime.Listener.class).forEach(l -> {
            if (l instanceof HasId) {
                String id = ((HasId) l).getId();
                if (!id.endsWith(".")) {
                    id = id + ".";
                }

                PropertiesSupport.bindProperties(getCamelContext(), l, id);
            }

            LOGGER.info("Adding listener: {}", l.getClass());
            listeners.add(l);
        });

        listeners.forEach(l -> l.accept(Phase.Starting, this));
        listeners.forEach(l -> l.accept(Phase.ConfigureContext, this));
        listeners.forEach(l -> l.accept(Phase.ConfigureRoutes, this));
    }

    public void started(@Observes StartedEvent event) {
        LOGGER.info("Camel started");
        listeners.forEach(l -> l.accept(Phase.Started, this));
    }

    public void stopping(@Observes StoppingEvent event) {
        LOGGER.info("Camel stopping");
        listeners.forEach(l -> l.accept(Phase.Stopping, this));
    }

    public void stopped(@Observes StoppedEvent event) {
        LOGGER.info("Camel stopped");
        listeners.forEach(l -> l.accept(Phase.Stopped, this));
    }

    @Override
    public CamelContext getCamelContext() {
        return runtime.getContext();
    }

    @Override
    public Registry getRegistry() {
        return runtime.getRegistry();
    }

}

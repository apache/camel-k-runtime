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
package org.apache.camel.k.cron;

import java.util.Optional;

import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.k.Runtime;
import org.apache.camel.k.RuntimeAware;
import org.apache.camel.k.Source;
import org.apache.camel.k.SourceLoader;
import org.apache.camel.k.annotation.LoaderInterceptor;
import org.apache.camel.k.support.RuntimeSupport;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.spi.CamelEvent;
import org.apache.camel.support.EventNotifierSupport;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@LoaderInterceptor("cron")
public class CronSourceLoaderInterceptor implements SourceLoader.Interceptor, RuntimeAware {

    private String timerUri;
    private String overridableComponents;
    private Runtime runtime;

    public CronSourceLoaderInterceptor() {
        this.timerUri = "timer:camel-k-cron-override?delay=0&period=1&repeatCount=1";
    }

    public String getTimerUri() {
        return timerUri;
    }

    public CronSourceLoaderInterceptor setTimerUri(String timerUri) {
        this.timerUri = timerUri;
        return this;
    }

    public String getOverridableComponents() {
        return overridableComponents;
    }

    public void setOverridableComponents(String overridableComponents) {
        this.overridableComponents = overridableComponents;
    }

    @Override
    public void setRuntime(Runtime runtime) {
        this.runtime = runtime;
    }

    @Override
    public Runtime getRuntime() {
        return runtime;
    }

    @Override
    public void beforeLoad(SourceLoader loader, Source source) {
    }

    @Override
    public SourceLoader.Result afterLoad(SourceLoader loader, Source source, SourceLoader.Result result) {
        return new SourceLoader.Result() {
            @Override
            public Optional<RoutesBuilder> builder() {
                return RuntimeSupport.afterConfigure(result.builder(), builder -> {
                    if (ObjectHelper.isEmpty(overridableComponents)) {
                        return;
                    }

                    CamelContext context = runtime.getCamelContext();
                    String[] components = overridableComponents.split(",", -1);

                    for (RouteDefinition def : builder.getRouteCollection().getRoutes()) {
                        String uri = def.getInput() != null ? def.getInput().getUri() : null;
                        if (shouldBeOverridden(uri, components)) {
                            def.getInput().setUri(timerUri);

                            //
                            // Don't install the shutdown strategy more than once.
                            //
                            if (context.getManagementStrategy().getEventNotifiers().stream().noneMatch(CronShutdownStrategy.class::isInstance)) {
                                context.getManagementStrategy().addEventNotifier(new CronShutdownStrategy(runtime));
                            }
                        }
                    }
                });
            }

            @Override
            public Optional<Object> configuration() {
                return result.configuration();
            }
        };
    }

    private static boolean shouldBeOverridden(String uri, String[] components) {
        if (uri == null) {
            return false;
        }
        for (String c : components) {
            if (uri.startsWith(c + ":")) {
                return true;
            }
        }
        return false;
    }

    private static class CronShutdownStrategy extends EventNotifierSupport {
        private static final Logger LOGGER = LoggerFactory.getLogger(CronShutdownStrategy.class);
        private final Runtime runtime;

        public CronShutdownStrategy(Runtime runtime) {
            this.runtime = runtime;
        }

        @Override
        public void notify(CamelEvent event) throws Exception {
            LOGGER.info("Initiate runtime shutdown");
            this.runtime.getCamelContext().getExecutorServiceManager().newThread("CronShutdownStrategy", () -> {
                try {
                    LOGGER.info("Shutting down the runtime");
                    runtime.stop();
                } catch (Exception e) {
                    LOGGER.warn("Error while shutting down the runtime", e);
                }
            }).start();
        }

        @Override
        public boolean isEnabled(CamelEvent event) {
            return event instanceof CamelEvent.ExchangeCompletedEvent || event instanceof CamelEvent.ExchangeFailedEvent;
        }
    }
}

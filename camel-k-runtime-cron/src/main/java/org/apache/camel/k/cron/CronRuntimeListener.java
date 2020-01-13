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

import java.util.List;

import org.apache.camel.k.Runtime;
import org.apache.camel.k.listener.AbstractPhaseListener;
import org.apache.camel.k.listener.RoutesConfigurer;
import org.apache.camel.model.Model;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CronRuntimeListener extends AbstractPhaseListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoutesConfigurer.class);

    private static final String ENV_CAMEL_K_CRON_OVERRIDE = "CAMEL_K_CRON_OVERRIDE";
    private static final String PROPERTY_CAMEL_K_CRON_OVERRIDE = "camel.k.cron.override";

    public CronRuntimeListener() {
        super(Runtime.Phase.ConfigureContext);
    }

    @Override
    protected void accept(Runtime runtime) {
        String components = System.getProperty(PROPERTY_CAMEL_K_CRON_OVERRIDE);

        if (ObjectHelper.isEmpty(components)) {
            components = System.getenv(ENV_CAMEL_K_CRON_OVERRIDE);
        }

        if (ObjectHelper.isEmpty(components)) {
            LOGGER.warn("No components to override found in {} environment variable", ENV_CAMEL_K_CRON_OVERRIDE);
            return;
        }

        // Add the cron route policy if there's at least one component to override
        runtime.getCamelContext().addRoutePolicyFactory(new CronRoutePolicyFactory(runtime));

        // Override components
        overrideCron(runtime, components.split(",", -1));
    }

    protected void overrideCron(Runtime runtime, String[] components) {
        List<RouteDefinition> definitions = runtime.getCamelContext().getExtension(Model.class).getRouteDefinitions();
        for (RouteDefinition def : definitions) {
            String uri = def.getInput() != null ? def.getInput().getUri() : null;
            if (shouldBeOverridden(uri, components)) {
                def.getInput().setUri("timer:camel-k-cron-override?delay=0&period=1&repeatCount=1");
            }
        }
    }

    protected boolean shouldBeOverridden(String uri, String[] components) {
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

}

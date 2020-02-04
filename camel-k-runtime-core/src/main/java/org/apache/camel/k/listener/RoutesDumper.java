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
package org.apache.camel.k.listener;

import org.apache.camel.CamelContext;
import org.apache.camel.k.Runtime;
import org.apache.camel.model.Model;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.RoutesDefinition;
import org.apache.camel.model.rest.RestDefinition;
import org.apache.camel.model.rest.RestsDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoutesDumper extends AbstractPhaseListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoutesDumper.class);

    public RoutesDumper() {
        super(Runtime.Phase.Started);
    }

    @Override
    protected void accept(Runtime runtime) {
        CamelContext context = runtime.getCamelContext();

        RoutesDefinition routes = new RoutesDefinition();
        routes.setRoutes(context.getExtension(Model.class).getRouteDefinitions());

        RestsDefinition rests = new RestsDefinition();
        rests.setRests(context.getExtension(Model.class).getRestDefinitions());

        if (LOGGER.isDebugEnabled() && !routes.getRoutes().isEmpty()) {
            for (RouteDefinition definition: routes.getRoutes()) {
                LOGGER.debug("Routes: {}", definition);
            }
        }
        if (LOGGER.isDebugEnabled() && !rests.getRests().isEmpty()) {
            for (RestDefinition definition: rests.getRests()) {
                LOGGER.debug("Rest: {}", definition);
            }
        }
    }
}

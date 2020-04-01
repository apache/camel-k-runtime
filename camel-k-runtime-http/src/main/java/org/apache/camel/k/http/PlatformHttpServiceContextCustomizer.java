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
package org.apache.camel.k.http;

import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.Ordered;
import org.apache.camel.component.platform.http.PlatformHttpComponent;
import org.apache.camel.component.platform.http.PlatformHttpConstants;
import org.apache.camel.k.ContextCustomizer;
import org.apache.camel.k.http.engine.RuntimePlatformHttpEngine;

public class PlatformHttpServiceContextCustomizer extends PlatformHttpServiceConfiguration implements ContextCustomizer {
    private PlatformHttpServiceEndpoint endpoint;

    @Override
    public int getOrder() {
        return Ordered.HIGHEST;
    }

    @Override
    public void apply(CamelContext camelContext) {
        endpoint = new PlatformHttpServiceEndpoint(camelContext, this);

        try {
            camelContext.addService(endpoint, true, true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // add the platform-http component
        PlatformHttpComponent component = new PlatformHttpComponent() {
            @Override
            protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
                // remove matchOnUriPrefix as it will be fixed by camel 3.2 but will cause the context
                // to fail as the property cannot be bound to the enpoint.
                //
                // TODO: remove once migrating to camel 3.2
                parameters.remove("matchOnUriPrefix");

                // let the original component to create the endpoint
                return super.createEndpoint(uri, remaining, parameters);
            }
        };

        component.setEngine(new RuntimePlatformHttpEngine());

        camelContext.addComponent(PlatformHttpConstants.PLATFORM_HTTP_COMPONENT_NAME, component);
    }
}

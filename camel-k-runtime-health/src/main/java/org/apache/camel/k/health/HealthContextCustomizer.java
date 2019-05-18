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
package org.apache.camel.k.health;

import org.apache.camel.CamelContext;
import org.apache.camel.k.ContextCustomizer;
import org.apache.camel.k.Runtime;
import org.apache.camel.k.servlet.ServletRegistration;

public class HealthContextCustomizer implements ContextCustomizer {
    public static final String DEFAULT_PATH = "/health";

    private String path;

    public HealthContextCustomizer() {
        this.path = DEFAULT_PATH;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public void apply(CamelContext camelContext, Runtime.Registry registry) {
        registry.bind(
            "health-servlet",
            new ServletRegistration(
                "HealthServlet",
                new HealthEndpoint(camelContext),
                path
            )
        );
    }
}

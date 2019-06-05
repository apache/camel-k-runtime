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

import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.core.ManagedServlet;
import io.undertow.servlet.core.ManagedServlets;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.k.Runtime;
import org.apache.camel.k.servlet.ServletContextCustomizer;
import org.apache.camel.test.AvailablePortFinder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class HealthCustomizerTest {

    @Test
    public void testServletConfigurer() {
        Runtime runtime = Runtime.of(new DefaultCamelContext());

        HealthContextCustomizer healthCustomizer = new HealthContextCustomizer();
        healthCustomizer.apply(runtime.getCamelContext());

        ServletContextCustomizer servletCustomizer = new ServletContextCustomizer();
        servletCustomizer.setBindPort(AvailablePortFinder.getNextAvailable());
        servletCustomizer.apply(runtime.getCamelContext());

        DeploymentManager manager = Servlets.defaultContainer().getDeploymentByPath("/");
        ManagedServlets managedServlets = manager.getDeployment().getServlets();
        ManagedServlet servlet = managedServlets.getManagedServlet("HealthServlet");

        assertThat(servlet).isNotNull();
        assertThat(servlet.getServletInfo().getMappings()).contains("/health");
    }
}

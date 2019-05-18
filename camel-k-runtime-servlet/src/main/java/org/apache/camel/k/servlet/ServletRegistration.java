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
package org.apache.camel.k.servlet;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.servlet.Servlet;

/**
 * An helper class used to register servlets.
 *
 * <pre>
 * public class WebhookCustomizer implements ContextCustomizer {
 *     @Override
 *     public void apply(CamelContext camelContext, Runtime.Registry registry) {
 *         registry.bind(
 *             "webhook-servlet",
 *             new ServletRegistration("CamelServlet", new CamelHttpTransportServlet(), "/webhook/*")
 *         );
 *     }
 * }
 * <pre>
 */
public final class ServletRegistration {
    private final Servlet servlet;
    private final String name;
    private final Set<String> mappings;

    public ServletRegistration(String name, Servlet servlet, Collection<String> mappings) {
        this.name = name;
        this.servlet = servlet;
        this.mappings = new LinkedHashSet<>();
        this.mappings.addAll(mappings);
    }

    public ServletRegistration(String name, Servlet servlet, String... mappings) {
        this.name = name;
        this.servlet = servlet;
        this.mappings = new LinkedHashSet<>();

        for (String mapping: mappings) {
            this.mappings.add(mapping);
        }
    }

    public String getName() {
        return name;
    }

    public Servlet getServlet() {
        return servlet;
    }

    public Collection<String> getMappings() {
        return mappings;
    }

    @Override
    public String toString() {
        return "ServletRegistration{" +
            "servlet=" + servlet +
            ", name='" + name + '\'' +
            ", mappings=" + mappings +
            '}';
    }
}

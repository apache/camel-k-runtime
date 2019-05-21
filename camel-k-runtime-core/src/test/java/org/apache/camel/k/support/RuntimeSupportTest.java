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
package org.apache.camel.k.support;

import java.util.List;
import java.util.Properties;

import org.apache.camel.CamelContext;
import org.apache.camel.Ordered;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.ExplicitCamelContextNameStrategy;
import org.apache.camel.k.Constants;
import org.apache.camel.k.ContextCustomizer;
import org.apache.camel.k.InMemoryRegistry;
import org.apache.camel.k.Runtime;
import org.apache.camel.spi.RestConfiguration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RuntimeSupportTest {

    @Test
    public void testLoadCustomizersWithPropertiesFlags() {
        PropertiesComponent pc = new PropertiesComponent();
        Runtime.Registry registry = new InMemoryRegistry();
        CamelContext context = new DefaultCamelContext(registry);
        context.addComponent("properties", pc);

        NameCustomizer customizer = new NameCustomizer("from-registry");
        registry.bind("name", customizer);

        List<ContextCustomizer> customizers = RuntimeSupport.configureContext(context, registry);
        assertThat(context.getName()).isNotEqualTo("from-registry");
        assertThat(context.getName()).isNotEqualTo("default");
        assertThat(customizers).hasSize(0);

        Properties properties = new Properties();
        properties.setProperty("customizer.name.enabled", "true");
        pc.setInitialProperties(properties);

        customizers = RuntimeSupport.configureContext(context, registry);
        assertThat(context.getName()).isEqualTo("from-registry");
        assertThat(customizers).hasSize(1);
    }

    @Test
    public void testLoadCustomizersWithList() {
        PropertiesComponent pc = new PropertiesComponent();
        Runtime.Registry registry = new InMemoryRegistry();
        CamelContext context = new DefaultCamelContext(registry);
        context.addComponent("properties", pc);

        NameCustomizer customizer = new NameCustomizer("from-registry");
        registry.bind("name", customizer);

        List<ContextCustomizer> customizers = RuntimeSupport.configureContext(context, registry);
        assertThat(context.getName()).isNotEqualTo("from-registry");
        assertThat(context.getName()).isNotEqualTo("default");
        assertThat(customizers).hasSize(0);

        Properties properties = new Properties();
        properties.setProperty(Constants.PROPERTY_CAMEL_K_CUSTOMIZER, "name");
        pc.setInitialProperties(properties);

        customizers = RuntimeSupport.configureContext(context, registry);
        assertThat(context.getName()).isEqualTo("from-registry");
        assertThat(customizers).hasSize(1);
    }

    @Test
    public void testLoadCustomizers() {
        PropertiesComponent pc = new PropertiesComponent();
        Runtime.Registry registry = new InMemoryRegistry();
        CamelContext context = new DefaultCamelContext(registry);
        context.addComponent("properties", pc);

        registry.bind("converters", (ContextCustomizer) (camelContext, registry1) -> camelContext.setLoadTypeConverters(false));

        List<ContextCustomizer> customizers = RuntimeSupport.configureContext(context, registry);
        assertThat(context.getName()).isNotEqualTo("from-registry");
        assertThat(context.getName()).isNotEqualTo("default");
        assertThat(context.isLoadTypeConverters()).isTrue();
        assertThat(customizers).hasSize(0);

        Properties properties = new Properties();
        properties.setProperty("customizer.name.enabled", "true");
        pc.setInitialProperties(properties);

        customizers = RuntimeSupport.configureContext(context, registry);
        assertThat(context.getName()).isEqualTo("default");
        assertThat(customizers).hasSize(1);

        properties.setProperty("customizer.converters.enabled", "true");
        pc.setInitialProperties(properties);

        customizers = RuntimeSupport.configureContext(context, registry);
        assertThat(context.getName()).isEqualTo("default");
        assertThat(context.isLoadTypeConverters()).isFalse();
        assertThat(customizers).hasSize(2);
    }

    @Test
    public void testLoadCustomizerOrder() {
        PropertiesComponent pc = new PropertiesComponent();
        Runtime.Registry registry = new InMemoryRegistry();
        DefaultCamelContext context = new DefaultCamelContext(registry);
        context.setName("camel");
        context.addComponent("properties", pc);

        registry.bind("c1", new ContextCustomizer() {
            @Override
            public int getOrder() {
                return Ordered.LOWEST;
            }

            @Override
            public void apply(CamelContext camelContext, Runtime.Registry registry) {
                camelContext.setNameStrategy(new ExplicitCamelContextNameStrategy(camelContext.getName() + "-c1"));
            }
        });
        registry.bind("c2", new ContextCustomizer() {
            @Override
            public int getOrder() {
                return Ordered.HIGHEST;
            }

            @Override
            public void apply(CamelContext camelContext, Runtime.Registry registry) {
                camelContext.setNameStrategy(new ExplicitCamelContextNameStrategy(camelContext.getName() + "-c2"));
            }
        });
        registry.bind("c3", new ContextCustomizer() {
            @Override
            public void apply(CamelContext camelContext, Runtime.Registry registry) {
                camelContext.setNameStrategy(new ExplicitCamelContextNameStrategy(camelContext.getName() + "-c3"));
            }
        });

        Properties properties = new Properties();
        properties.setProperty("customizer.c1.enabled", "true");
        properties.setProperty("customizer.c2.enabled", "true");
        properties.setProperty("customizer.c3.enabled", "true");
        pc.setInitialProperties(properties);

        List<ContextCustomizer> customizers = RuntimeSupport.configureContext(context, registry);
        assertThat(customizers).hasSize(3);
        assertThat(context.getName()).isEqualTo("camel-c2-c3-c1");
    }

    @Test
    public void testCustomizeRestConfiguration() {
        Properties properties = new Properties();
        properties.setProperty("camel.rest.component", "servlet");
        properties.setProperty("camel.rest.contextPath", "/mypath");
        properties.setProperty(Constants.PROPERTY_PREFIX_REST_COMPONENT_PROPERTY + "servletName", "MyCamelServlet");
        properties.setProperty(Constants.PROPERTY_PREFIX_REST_ENDPOINT_PROPERTY  + "headerFilterStrategy", "myHeaderStrategy");

        PropertiesComponent pc = new PropertiesComponent();
        pc.setInitialProperties(properties);

        Runtime.Registry registry = new InMemoryRegistry();
        CamelContext context = new DefaultCamelContext(registry);
        context.addComponent("properties", pc);


        RuntimeSupport.configureRest(context);

        RestConfiguration configuration = context.getRestConfiguration();
        assertThat(configuration).hasFieldOrPropertyWithValue("component", "servlet");
        assertThat(configuration).hasFieldOrPropertyWithValue("contextPath", "/mypath");
        assertThat(configuration.getComponentProperties()).containsEntry("servletName", "MyCamelServlet");
        assertThat(configuration.getEndpointProperties()).containsEntry("headerFilterStrategy", "myHeaderStrategy");
    }
}

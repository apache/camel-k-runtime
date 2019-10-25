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
package org.apache.camel.k.support;

import java.util.List;
import java.util.Properties;

import org.apache.camel.CamelContext;
import org.apache.camel.Ordered;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.engine.ExplicitCamelContextNameStrategy;
import org.apache.camel.k.Constants;
import org.apache.camel.k.ContextCustomizer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RuntimeSupportTest {

    @Test
    public void testLoadCustomizersWithPropertiesFlags() {
        CamelContext context = new DefaultCamelContext();

        NameCustomizer customizer = new NameCustomizer("from-registry");
        context.getRegistry().bind("name", customizer);

        List<ContextCustomizer> customizers = RuntimeSupport.configureContextCustomizers(context);
        assertThat(context.getName()).isNotEqualTo("from-registry");
        assertThat(context.getName()).isNotEqualTo("default");
        assertThat(customizers).hasSize(0);

        Properties properties = new Properties();
        properties.setProperty("customizer.name.enabled", "true");
        context.getPropertiesComponent().setInitialProperties(properties);

        customizers = RuntimeSupport.configureContextCustomizers(context);
        assertThat(context.getName()).isEqualTo("from-registry");
        assertThat(customizers).hasSize(1);
    }

    @Test
    public void testLoadCustomizersWithList() {
        CamelContext context = new DefaultCamelContext();

        NameCustomizer customizer = new NameCustomizer("from-registry");
        context.getRegistry().bind("name", customizer);

        List<ContextCustomizer> customizers = RuntimeSupport.configureContextCustomizers(context);
        assertThat(context.getName()).isNotEqualTo("from-registry");
        assertThat(context.getName()).isNotEqualTo("default");
        assertThat(customizers).hasSize(0);

        Properties properties = new Properties();
        properties.setProperty(Constants.PROPERTY_CAMEL_K_CUSTOMIZER, "name");
        context.getPropertiesComponent().setInitialProperties(properties);

        customizers = RuntimeSupport.configureContextCustomizers(context);
        assertThat(context.getName()).isEqualTo("from-registry");
        assertThat(customizers).hasSize(1);
    }

    @Test
    public void testLoadCustomizers() {
        CamelContext context = new DefaultCamelContext();
        context.getRegistry().bind("converters", (ContextCustomizer) (camelContext) -> camelContext.setLoadTypeConverters(false));

        List<ContextCustomizer> customizers = RuntimeSupport.configureContextCustomizers(context);
        assertThat(context.getName()).isNotEqualTo("from-registry");
        assertThat(context.getName()).isNotEqualTo("default");
        assertThat(context.isLoadTypeConverters()).isTrue();
        assertThat(customizers).hasSize(0);

        Properties properties = new Properties();
        properties.setProperty("customizer.name.enabled", "true");
        context.getPropertiesComponent().setInitialProperties(properties);

        customizers = RuntimeSupport.configureContextCustomizers(context);
        assertThat(context.getName()).isEqualTo("default");
        assertThat(customizers).hasSize(1);

        properties.setProperty("customizer.converters.enabled", "true");
        context.getPropertiesComponent().setInitialProperties(properties);

        customizers = RuntimeSupport.configureContextCustomizers(context);
        assertThat(context.getName()).isEqualTo("default");
        assertThat(context.isLoadTypeConverters()).isFalse();
        assertThat(customizers).hasSize(2);
    }

    @Test
    public void testLoadCustomizerOrder() {
        DefaultCamelContext context = new DefaultCamelContext();
        context.setName("camel");
        context.getRegistry().bind("c1", new ContextCustomizer() {
            @Override
            public int getOrder() {
                return Ordered.LOWEST;
            }

            @Override
            public void apply(CamelContext camelContext) {
                camelContext.setNameStrategy(new ExplicitCamelContextNameStrategy(camelContext.getName() + "-c1"));
            }
        });
        context.getRegistry().bind("c2", new ContextCustomizer() {
            @Override
            public int getOrder() {
                return Ordered.HIGHEST;
            }

            @Override
            public void apply(CamelContext camelContext) {
                camelContext.setNameStrategy(new ExplicitCamelContextNameStrategy(camelContext.getName() + "-c2"));
            }
        });
        context.getRegistry().bind("c3", new ContextCustomizer() {
            @Override
            public void apply(CamelContext camelContext) {
                camelContext.setNameStrategy(new ExplicitCamelContextNameStrategy(camelContext.getName() + "-c3"));
            }
        });

        Properties properties = new Properties();
        properties.setProperty("customizer.c1.enabled", "true");
        properties.setProperty("customizer.c2.enabled", "true");
        properties.setProperty("customizer.c3.enabled", "true");

        context.getPropertiesComponent().setInitialProperties(properties);

        List<ContextCustomizer> customizers = RuntimeSupport.configureContextCustomizers(context);
        assertThat(customizers).hasSize(3);
        assertThat(context.getName()).isEqualTo("camel-c2-c3-c1");
    }
}

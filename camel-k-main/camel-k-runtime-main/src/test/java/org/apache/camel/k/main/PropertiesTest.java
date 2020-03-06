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
package org.apache.camel.k.main;

import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.apache.camel.CamelContext;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.component.seda.SedaComponent;
import org.apache.camel.k.Constants;
import org.apache.camel.k.ContextCustomizer;
import org.apache.camel.k.Runtime;
import org.apache.camel.k.listener.ContextConfigurer;
import org.apache.camel.k.support.PropertiesSupport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PropertiesTest {

    @Test
    public void testLoadProperties() throws Exception {
        System.setProperty(Constants.PROPERTY_CAMEL_K_CONF, "src/test/resources/conf.properties");
        System.setProperty(Constants.PROPERTY_CAMEL_K_CONF_D, "src/test/resources/conf.d");

        try {
            ApplicationRuntime runtime = new ApplicationRuntime();
            runtime.setInitialProperties(PropertiesSupport.loadApplicationProperties());
            runtime.setPropertiesLocations(PropertiesSupport.resolveUserPropertiesLocations());
            runtime.addListener(new ContextConfigurer());
            runtime.addListener(Runtime.Phase.Started, r -> {
                final CamelContext context = r.getCamelContext();
                final PropertiesComponent pc = (PropertiesComponent)context.getPropertiesComponent();

                assertThat(pc.getInitialProperties()).containsExactlyEntriesOf(PropertiesSupport.loadApplicationProperties());
                assertThat(pc.getInitialProperties().getProperty("root.key")).isEqualTo("root.value");
                assertThat(pc.getInitialProperties().getProperty("a.key")).isEqualTo("a.root");

                assertThat(pc.getLocations()).hasSameElementsAs(
                    PropertiesSupport.resolveUserPropertiesLocations().stream()
                        .map(location -> "file:" + location)
                        .distinct()
                        .sorted()
                        .collect(Collectors.toList())
                );

                assertThat(pc.resolveProperty("root.key")).get().isEqualTo("root.value");
                assertThat(pc.resolveProperty("001.key")).get().isEqualTo("001.value");
                assertThat(pc.resolveProperty("002.key")).get().isEqualTo("002.value");
                assertThat(pc.resolveProperty("a.key")).get().isEqualTo("a.002");
                runtime.stop();
            });

            runtime.run();
        } finally {
            System.getProperties().remove(Constants.PROPERTY_CAMEL_K_CONF);
            System.getProperties().remove(Constants.PROPERTY_CAMEL_K_CONF_D);
        }
    }

    @Test
    public void testSystemProperties() throws Exception {
        System.setProperty("my.property", "my.value");

        try {
            ApplicationRuntime runtime = new ApplicationRuntime();
            runtime.setProperties(System.getProperties());
            runtime.addListener(new ContextConfigurer());
            runtime.addListener(Runtime.Phase.Started, r -> {
                CamelContext context = r.getCamelContext();
                String value = context.resolvePropertyPlaceholders("{{my.property}}");

                assertThat(value).isEqualTo("my.value");
                runtime.stop();
            });

            runtime.run();
        } finally {
            System.getProperties().remove("my.property");
        }
    }

    @Test
    public void testComponentConfiguration() throws Exception {
        int queueSize1 = ThreadLocalRandom.current().nextInt(10, 100);
        int queueSize2 = ThreadLocalRandom.current().nextInt(10, 100);

        Properties properties = new Properties();
        properties.setProperty("camel.component.seda.queueSize", Integer.toString(queueSize1));
        properties.setProperty("camel.component.my-seda.queueSize", Integer.toString(queueSize2));

        ApplicationRuntime runtime = new ApplicationRuntime();
        runtime.setProperties(properties);
        runtime.getRegistry().bind("my-seda", new SedaComponent());
        runtime.addListener(new ContextConfigurer());
        runtime.addListener(Runtime.Phase.Started, r -> {
            CamelContext context = r.getCamelContext();
            assertThat(context.getComponent("seda", true)).hasFieldOrPropertyWithValue("queueSize", queueSize1);
            assertThat(context.getComponent("my-seda", true)).hasFieldOrPropertyWithValue("queueSize", queueSize2);
            runtime.stop();
        });

        runtime.run();
    }

    @Test
    public void testContextConfiguration() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("camel.context.message-history", "false");
        properties.setProperty("camel.context.load-type-converters", "false");
        properties.setProperty("camel.context.stream-caching-strategy.spool-threshold", "100");
        properties.setProperty("camel.context.rest-configuration.component", "servlet");
        properties.setProperty("camel.context.rest-configuration.context-path", "/my/path");

        ApplicationRuntime runtime = new ApplicationRuntime();
        runtime.setProperties(properties);
        runtime.addListener(new ContextConfigurer());
        runtime.addListener(Runtime.Phase.Started, r -> {
            CamelContext context = r.getCamelContext();
            assertThat(context.isMessageHistory()).isFalse();
            assertThat(context.isLoadTypeConverters()).isFalse();
            assertThat(context.getStreamCachingStrategy().getSpoolThreshold()).isEqualTo(100);
            assertThat(context.getRestConfiguration().getComponent()).isEqualTo("servlet");
            assertThat(context.getRestConfiguration().getContextPath()).isEqualTo("/my/path");
            runtime.stop();
        });

        runtime.run();
    }

    @Test
    public void testContextCustomizerFromProperty() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("customizer.test.enabled", "true");
        properties.setProperty("customizer.test.message-history", "false");

        ApplicationRuntime runtime = new ApplicationRuntime();
        runtime.setProperties(properties);
        runtime.addListener(new ContextConfigurer());
        runtime.addListener(Runtime.Phase.Started, r -> {
            CamelContext context = r.getCamelContext();
            assertThat(context.isMessageHistory()).isFalse();
            assertThat(context.isLoadTypeConverters()).isFalse();
            runtime.stop();
        });

        runtime.run();
    }

    @Test
    public void testContextCustomizerFromRegistry() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("customizer.c1.enabled", "true");

        ApplicationRuntime runtime = new ApplicationRuntime();
        runtime.setProperties(properties);
        runtime.addListener(new ContextConfigurer());
        runtime.getRegistry().bind("c1", (ContextCustomizer) camelContext -> {
            camelContext.setMessageHistory(false);
            camelContext.setLoadTypeConverters(false);
        });
        runtime.addListener(Runtime.Phase.Started, r -> {
            CamelContext context = r.getCamelContext();
            assertThat(context.isMessageHistory()).isFalse();
            assertThat(context.isLoadTypeConverters()).isFalse();
            runtime.stop();
        });

        runtime.run();
    }
}

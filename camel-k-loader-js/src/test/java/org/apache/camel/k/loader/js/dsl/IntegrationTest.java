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
package org.apache.camel.k.loader.js.dsl;

import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.seda.SedaComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.k.Runtime;
import org.apache.camel.k.listener.RoutesConfigurer;
import org.apache.camel.model.FromDefinition;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.TransformDefinition;
import org.apache.camel.model.rest.GetVerbDefinition;
import org.apache.camel.model.rest.RestDefinition;
import org.apache.camel.spi.RestConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class IntegrationTest {
    private CamelContext context;
    private Runtime runtime;

    @BeforeEach
    public void setUp() {
        this.context = new DefaultCamelContext();
        this.runtime = Runtime.on(context);
    }

    @AfterEach
    public void shutDown() {
        if (this.context != null) {
            this.context.stop();
        }
    }

    private void configureRoutes(String... routes) {
        RoutesConfigurer.forRoutes(routes).accept(Runtime.Phase.ConfigureRoutes, runtime);
    }

    @Test
    public void testComponentConfiguration() {
        configureRoutes(
            "classpath:routes-with-component-configuration.js"
        );

        SedaComponent seda = context.getComponent("seda", SedaComponent.class);

        assertThat(seda).isNotNull();
        assertThat(seda).hasFieldOrPropertyWithValue("queueSize", 1234);
    }

    @Test
    public void testRestConfiguration() {
        configureRoutes(
            "classpath:routes-with-rest-configuration.js"
        );

        RestConfiguration conf = context.getRestConfiguration();

        assertThat(conf).isNotNull();
        assertThat(conf).hasFieldOrPropertyWithValue("component", "undertow");
        assertThat(conf).hasFieldOrPropertyWithValue("port", 1234);
    }

    @Test
    public void testRestDSL() {
        configureRoutes(
            "classpath:routes-with-rest-dsl.js"
        );

        ModelCamelContext mcc = context.adapt(ModelCamelContext.class);
        List<RestDefinition> rests = mcc.getRestDefinitions();
        List<RouteDefinition> routes = mcc.getRouteDefinitions();

        assertThat(rests).hasSize(1);
        assertThat(rests).first().hasFieldOrPropertyWithValue("produces", "text/plain");
        assertThat(rests).first().satisfies(definition -> {
            assertThat(definition.getVerbs()).hasSize(1);
            assertThat(definition.getVerbs()).first().isInstanceOfSatisfying(GetVerbDefinition.class, get -> {
                assertThat(get).hasFieldOrPropertyWithValue("uri", "/say/hello");
            });
        });

        assertThat(routes).hasSize(1);
        assertThat(routes).first().satisfies(definition -> {
            assertThat(definition.getInput()).isInstanceOf(FromDefinition.class);
            assertThat(definition.getOutputs()).hasSize(1);
            assertThat(definition.getOutputs()).first().satisfies(output -> {
                assertThat(output).isInstanceOf(TransformDefinition.class);
            });
        });
    }

    @Test
    public void testProcessors() {
        configureRoutes(
            "classpath:routes-with-processors.js"
        );

        context.start();

        ProducerTemplate template = context.createProducerTemplate();

        assertThat(template.requestBody("direct:arrow", "")).isEqualTo("arrow");
        assertThat(template.requestBody("direct:wrapper", "")).isEqualTo("wrapper");
        assertThat(template.requestBody("direct:function", "")).isEqualTo("function");
    }


    @Test
    public void testContextConfiguration() {
        configureRoutes(
            "classpath:routes-with-context-configuration.js"
        );

        assertThat(context.isTypeConverterStatisticsEnabled()).isTrue();
    }
}

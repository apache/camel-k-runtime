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

import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.seda.SedaComponent;
import org.apache.camel.k.Runtime;
import org.apache.camel.k.listener.RoutesConfigurer;
import org.apache.camel.k.main.ApplicationRuntime;
import org.apache.camel.model.FromDefinition;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.TransformDefinition;
import org.apache.camel.model.rest.GetVerbDefinition;
import org.apache.camel.model.rest.RestDefinition;
import org.apache.camel.spi.RestConfiguration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class IntegrationTest {
    @Test
    public void testComponentConfiguration() throws Exception {
        ApplicationRuntime runtime = new ApplicationRuntime();
        runtime.addListener(RoutesConfigurer.forRoutes("classpath:routes-with-component-configuration.js"));
        runtime.addListener(Runtime.Phase.Started, r -> {
            SedaComponent seda = r.getCamelContext().getComponent("seda", SedaComponent.class);

            assertThat(seda).isNotNull();
            assertThat(seda).hasFieldOrPropertyWithValue("queueSize", 1234);

            runtime.stop();
        });

        runtime.run();
    }

    @Test
    public void testRestConfiguration() throws Exception {
        ApplicationRuntime runtime = new ApplicationRuntime();
        runtime.addListener(RoutesConfigurer.forRoutes("classpath:routes-with-rest-configuration.js"));
        runtime.addListener(Runtime.Phase.Started, r -> {
            RestConfiguration conf = r.getCamelContext().getRestConfiguration();

            assertThat(conf).isNotNull();
            assertThat(conf).hasFieldOrPropertyWithValue("component", "undertow");
            assertThat(conf).hasFieldOrPropertyWithValue("port", 1234);

            runtime.stop();
        });

        runtime.run();
    }

    @Test
    public void testRestDSL() throws Exception {
        ApplicationRuntime runtime = new ApplicationRuntime();
        runtime.addListener(RoutesConfigurer.forRoutes("classpath:routes-with-rest-dsl.js"));
        runtime.addListener(Runtime.Phase.Started, r -> {
            ModelCamelContext mcc = r.getCamelContext().adapt(ModelCamelContext.class);
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

            runtime.stop();
        });

        runtime.run();
    }

    @Test
    public void testProcessors() throws Exception {
        ApplicationRuntime runtime = new ApplicationRuntime();
        runtime.addListener(RoutesConfigurer.forRoutes("classpath:routes-with-processors.js"));
        runtime.addListener(Runtime.Phase.Started, r -> {
            ProducerTemplate template = r.getCamelContext().createProducerTemplate();

            String a = template.requestBody("direct:arrow", "", String.class);
            assertThat(a).isEqualTo("arrow");

            String f = template.requestBody("direct:function", "", String.class);
            assertThat(f).isEqualTo("function");

            runtime.stop();
        });

        runtime.run();
    }
}

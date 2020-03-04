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
package org.apache.camel.k.knative.yaml.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.k.Runtime;
import org.apache.camel.k.Source;
import org.apache.camel.k.SourceLoader;
import org.apache.camel.k.Sources;
import org.apache.camel.k.listener.RoutesConfigurer;
import org.apache.camel.k.loader.yaml.YamlSourceLoader;
import org.apache.camel.model.FromDefinition;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.ToDefinition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class KnativeConverterTest {

    @Test
    public void testLoadRoutes() throws Exception {
        TestRuntime runtime = new TestRuntime();
        Source source = Sources.fromURI("classpath:route.yaml");
        SourceLoader loader = RoutesConfigurer.load(runtime, source);

        assertThat(loader).isInstanceOf(YamlSourceLoader.class);
        assertThat(runtime.builders).hasSize(1);

        runtime.camelContext.addRoutes(runtime.builders.get(0));

        List<RouteDefinition> routes = runtime.camelContext.getRouteDefinitions();
        assertThat(routes).hasSize(1);

        // definition
        assertThat(routes)
            .first()
            .extracting(RouteDefinition::getId)
            .isEqualTo("knative");
        assertThat(routes)
            .first()
            .extracting(RouteDefinition::getGroup)
            .isEqualTo("flows");

        // input
        assertThat(routes)
            .first()
            .extracting(RouteDefinition::getInput)
            .isInstanceOfSatisfying(FromDefinition.class, t-> {
                assertThat(t.getEndpointUri()).isEqualTo("knative:endpoint/from");
            });

        // outputs
        assertThat(routes)
            .first()
            .satisfies(KnativeConverterTest::validateSteps);
    }

    private static void validateSteps(RouteDefinition definition) {
        List<ProcessorDefinition<?>> outputs = definition.getOutputs();

        assertThat(outputs).hasSize(2);

        assertThat(outputs)
            .element(0)
            .isInstanceOfSatisfying(ToDefinition.class, t-> {
                assertThat(t.getEndpointUri()).isEqualTo("log:info");
            });
        assertThat(outputs)
            .element(1)
            .isInstanceOfSatisfying(ToDefinition.class, t-> {
                assertThat(t.getEndpointUri()).isEqualTo("knative:endpoint/to");
            });
    }

    static class TestRuntime implements Runtime {
        private final DefaultCamelContext camelContext;
        private final List<RoutesBuilder> builders;

        public TestRuntime() {
            this.camelContext = new DefaultCamelContext();
            this.builders = new ArrayList<>();
        }

        @Override
        public CamelContext getCamelContext() {
            return this.camelContext;
        }

        @Override
        public void addRoutes(RoutesBuilder builder) {
            this.builders.add(builder);
        }

        @Override
        public void setPropertiesLocations(Collection<String> locations) {
        }
    }
}

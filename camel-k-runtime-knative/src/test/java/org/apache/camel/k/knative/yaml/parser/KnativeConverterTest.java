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

import java.util.List;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.k.RoutesLoader;
import org.apache.camel.k.Source;
import org.apache.camel.k.Sources;
import org.apache.camel.k.loader.yaml.YamlRoutesLoader;
import org.apache.camel.k.support.RuntimeSupport;
import org.apache.camel.model.FromDefinition;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.ToDefinition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class KnativeConverterTest {

    @Test
    public void testLoadRoutes() throws Exception {
        DefaultCamelContext context = new DefaultCamelContext();
        Source source = Sources.fromURI("classpath:route.yaml");
        RoutesLoader loader = RuntimeSupport.loaderFor(context, source);
        RouteBuilder builder = loader.load(context, source);

        assertThat(loader).isInstanceOf(YamlRoutesLoader.class);
        assertThat(builder).isNotNull();

        context.addRoutes(builder);

        List<RouteDefinition> routes = context.getRouteDefinitions();
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
}

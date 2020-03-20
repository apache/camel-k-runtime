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
package org.apache.camel.k.loader.java;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.k.Runtime;
import org.apache.camel.k.Source;
import org.apache.camel.k.SourceLoader;
import org.apache.camel.k.Sources;
import org.apache.camel.k.listener.RoutesConfigurer;
import org.apache.camel.model.ProcessDefinition;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.SetBodyDefinition;
import org.apache.camel.model.ToDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

public class RoutesLoaderTest {
    @Test
    public void testLoadJavaWithNestedClass() throws Exception {
        TestRuntime runtime = new TestRuntime();
        Source source = Sources.fromURI("classpath:MyRoutesWithNestedClass.java");
        SourceLoader loader = RoutesConfigurer.load(runtime, source);

        assertThat(loader).isInstanceOf(JavaSourceLoader.class);
        assertThat(runtime.builders).hasSize(1);
        assertThat(runtime.builders).first().isInstanceOf(RouteBuilder.class);

        RouteBuilder builder = (RouteBuilder)runtime.builders.get(0);
        builder.setContext(runtime.getCamelContext());
        builder.configure();

        List<RouteDefinition> routes = builder.getRouteCollection().getRoutes();
        assertThat(routes).hasSize(1);
        assertThat(routes.get(0).getInput().getEndpointUri()).isEqualTo("timer:tick");
        assertThat(routes.get(0).getOutputs().get(0)).isInstanceOf(SetBodyDefinition.class);
        assertThat(routes.get(0).getOutputs().get(1)).isInstanceOf(ProcessDefinition.class);
        assertThat(routes.get(0).getOutputs().get(2)).isInstanceOf(ToDefinition.class);
    }

    @Test
    public void testLoadJavaWithRestConfiguration() throws Exception {
        TestRuntime runtime = new TestRuntime();
        Source source = Sources.fromURI("classpath:MyRoutesWithRestConfiguration.java");
        SourceLoader loader = RoutesConfigurer.load(runtime, source);

        assertThat(loader).isInstanceOf(JavaSourceLoader.class);
        assertThat(runtime.builders).hasSize(1);
        assertThat(runtime.builders).first().isInstanceOf(RouteBuilder.class);

        runtime.getCamelContext().addRoutes(runtime.builders.get(0));

        assertThat(runtime.getCamelContext().getRestConfiguration()).hasFieldOrPropertyWithValue("component", "restlet");
    }

    @Test
    public void testLoadJavaConfiguration() throws Exception {
        TestRuntime runtime = new TestRuntime();
        Source source = Sources.fromURI("classpath:MyRoutesConfig.java");
        SourceLoader loader = RoutesConfigurer.load(runtime, source);

        assertThat(loader).isInstanceOf(JavaSourceLoader.class);
        assertThat(runtime.builders).isEmpty();
        assertThat(runtime.configurations).hasSize(1);
    }

    @Test
    public void testLoadJavaWithModel() throws Exception {
        TestRuntime runtime = new TestRuntime();
        Source source = Sources.fromURI("classpath:MyRoutesWithModel.java");
        SourceLoader loader = RoutesConfigurer.load(runtime, source);

        assertThat(loader).isInstanceOf(JavaSourceLoader.class);
        assertThat(runtime.builders).hasSize(1);
        assertThat(runtime.builders).first().isInstanceOf(RouteBuilder.class);

        runtime.getCamelContext().addRoutes(runtime.builders.get(0));

        assertThat(runtime.camelContext.getRestDefinitions()).first().satisfies(definition -> {
            assertThat(definition.getVerbs()).first().satisfies(verb -> {
                assertThat(verb).hasFieldOrPropertyWithValue("outType", "org.apache.camel.k.loader.java.model.EmployeeDTO");
            });
        });
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testLoaders(String location, Class<? extends SourceLoader> type) throws Exception {
        TestRuntime runtime = new TestRuntime();
        Source source = Sources.fromURI(location);
        SourceLoader loader = RoutesConfigurer.load(runtime, source);

        assertThat(loader).isInstanceOf(type);
        assertThat(runtime.builders).hasSize(1);
        assertThat(runtime.builders).first().isInstanceOf(RouteBuilder.class);

        RouteBuilder builder = (RouteBuilder)runtime.builders.get(0);
        builder.setContext(runtime.getCamelContext());
        builder.configure();

        List<RouteDefinition> routes = builder.getRouteCollection().getRoutes();
        assertThat(routes).hasSize(1);
        assertThat(routes.get(0).getInput().getEndpointUri()).isEqualTo("timer:tick");
        assertThat(routes.get(0).getOutputs().get(0)).isInstanceOf(ToDefinition.class);
    }

    static Stream<Arguments> parameters() {
        return Stream.of(
            Arguments.arguments("classpath:MyRoutes.java", JavaSourceLoader.class),
            Arguments.arguments("classpath:MyRoutesWithNameOverride.java?name=MyRoutes.java", JavaSourceLoader.class),
            Arguments.arguments("classpath:MyRoutesWithPackage.java", JavaSourceLoader.class),
            Arguments.arguments("classpath:MyRoutesWithPackageAndComment.java", JavaSourceLoader.class),
            Arguments.arguments("classpath:MyRoutesWithPackageAndLineComment.java", JavaSourceLoader.class),
            Arguments.arguments("classpath:MyRoutesWithEndpointDsl.java", JavaSourceLoader.class)
        );
    }

    static class TestRuntime implements Runtime {
        private final DefaultCamelContext camelContext;
        private final List<RoutesBuilder> builders;
        private final List<Object> configurations;

        public TestRuntime() {
            this.camelContext = new DefaultCamelContext();
            this.builders = new ArrayList<>();
            this.configurations = new ArrayList<>();
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
        public void addConfiguration(Object configuration) {
            this.configurations.add(configuration);
        }

        @Override
        public void setPropertiesLocations(Collection<String> locations) {
        }
    }
}


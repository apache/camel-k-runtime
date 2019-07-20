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

import java.util.List;
import java.util.stream.Stream;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.k.RoutesLoader;
import org.apache.camel.k.Runtime;
import org.apache.camel.k.Source;
import org.apache.camel.k.listener.RoutesConfigurer;
import org.apache.camel.k.main.ApplicationRuntime;
import org.apache.camel.k.support.RuntimeSupport;
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
    public void testLoaderFromRegistry() throws Exception {
        RoutesLoader myLoader = new JavaClassRoutesLoader();
        CamelContext camelContext = new DefaultCamelContext();
        camelContext.getRegistry().bind("my-loader", myLoader);

        Source source = Source.create("classpath:" + MyRoutes.class.getName() + ".class");
        RoutesLoader loader = RuntimeSupport.loaderFor(camelContext, source);

        assertThat(loader).isInstanceOf(JavaClassRoutesLoader.class);
        assertThat(loader).isSameAs(myLoader);
    }

    @Test
    public void testLoadJavaWithNestedClass() throws Exception {
        CamelContext context = new DefaultCamelContext();

        Source source = Source.create("classpath:MyRoutesWithNestedClass.java");
        RoutesLoader loader = RuntimeSupport.loaderFor(new DefaultCamelContext(), source);
        RouteBuilder builder = loader.load(context, source);

        assertThat(loader).isInstanceOf(JavaSourceRoutesLoader.class);
        assertThat(builder).isNotNull();

        builder.setContext(context);
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
        CamelContext context = new DefaultCamelContext();

        Source source = Source.create("classpath:MyRoutesWithRestConfiguration.java");
        RoutesLoader loader = RuntimeSupport.loaderFor(new DefaultCamelContext(), source);
        RouteBuilder builder = loader.load(context, source);

        assertThat(loader).isInstanceOf(JavaSourceRoutesLoader.class);
        assertThat(builder).isNotNull();

        context.addRoutes(builder);

        assertThat(context.getRestConfigurations()).hasSize(1);
        assertThat(context.getRestConfigurations().iterator().next()).hasFieldOrPropertyWithValue("component", "restlet");
    }

    @Test
    public void testLoadJavaClassWithBeans() throws Exception {
        ApplicationRuntime runtime = new ApplicationRuntime();
        runtime.addListener(RoutesConfigurer.forRoutes("classpath:" + MyRoutesWithBeans.class.getName() + ".class"));
        runtime.addListener(Runtime.Phase.Started, r ->  runtime.stop());
        runtime.run();

        assertThat(runtime.getRegistry().lookupByName("my-bean")).isInstanceOfSatisfying(MyBean.class, b -> {
            assertThat(b).hasFieldOrPropertyWithValue("name", "my-bean-name");
        });
    }

    @Test
    public void testLoadJavaSourceWithBeans() throws Exception {
        ApplicationRuntime runtime = new ApplicationRuntime();
        runtime.addListener(RoutesConfigurer.forRoutes("classpath:MyRoutesWithBeans.java"));
        runtime.addListener(Runtime.Phase.Started, r ->  runtime.stop());
        runtime.run();

        assertThat(runtime.getRegistry().lookupByName("my-bean")).isInstanceOfSatisfying(MyBean.class, b -> {
            assertThat(b).hasFieldOrPropertyWithValue("name", "my-bean-name");
        });
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testLoaders(String location, Class<? extends RoutesLoader> type) throws Exception {
        Source source = Source.create(location);
        RoutesLoader loader = RuntimeSupport.loaderFor(new DefaultCamelContext(), source);
        RouteBuilder builder = loader.load(new DefaultCamelContext(), source);

        assertThat(loader).isInstanceOf(type);
        assertThat(builder).isNotNull();

        builder.setContext(new DefaultCamelContext());
        builder.configure();

        List<RouteDefinition> routes = builder.getRouteCollection().getRoutes();
        assertThat(routes).hasSize(1);
        assertThat(routes.get(0).getInput().getEndpointUri()).isEqualTo("timer:tick");
        assertThat(routes.get(0).getOutputs().get(0)).isInstanceOf(ToDefinition.class);
    }

    static Stream<Arguments> parameters() {
        return Stream.of(
            Arguments.arguments("classpath:" + MyRoutes.class.getName() + ".class", JavaClassRoutesLoader.class),
            Arguments.arguments("classpath:MyRoutes.java", JavaSourceRoutesLoader.class),
            Arguments.arguments("classpath:MyRoutesWithNameOverride.java?name=MyRoutes.java", JavaSourceRoutesLoader.class),
            Arguments.arguments("classpath:MyRoutesWithPackage.java", JavaSourceRoutesLoader.class)
        );
    }
}

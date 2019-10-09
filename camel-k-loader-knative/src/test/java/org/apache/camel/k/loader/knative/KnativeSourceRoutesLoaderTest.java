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
package org.apache.camel.k.loader.knative;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.knative.KnativeComponent;
import org.apache.camel.component.knative.spi.Knative;
import org.apache.camel.component.knative.spi.KnativeEnvironment;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.k.RoutesLoader;
import org.apache.camel.k.Source;
import org.apache.camel.k.Sources;
import org.apache.camel.k.support.RuntimeSupport;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.test.AvailablePortFinder;
import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class KnativeSourceRoutesLoaderTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(KnativeSourceRoutesLoaderTest.class);

    static Stream<Arguments> parameters() {
        return Stream.of(
            Arguments.arguments("classpath:routes.yaml?loader=knative-source"),
            Arguments.arguments("classpath:routes.yaml?language=knative-source-yaml"),
            Arguments.arguments("classpath:routes.xml?loader=knative-source"),
            Arguments.arguments("classpath:routes.xml?language=knative-source-xml"),
            Arguments.arguments("classpath:routes.groovy?loader=knative-source"),
            Arguments.arguments("classpath:routes.groovy?language=knative-source-groovy"),
            Arguments.arguments("classpath:routes.kts?loader=knative-source"),
            Arguments.arguments("classpath:routes.kts?language=knative-source-kts"),
            Arguments.arguments("classpath:routes.js?loader=knative-source"),
            Arguments.arguments("classpath:routes.js?language=knative-source-js"),
            Arguments.arguments("classpath:routes.java?name=MyRoutes.java&loader=knative-source"),
            Arguments.arguments("classpath:routes.java?name=MyRoutes.java&language=knative-source-java")
        ).sequential();
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testWrapLoader(String uri) throws Exception {
        LOGGER.info("uri: {}", uri);

        final int port = AvailablePortFinder.getNextAvailable();
        final String data = UUID.randomUUID().toString();

        KnativeComponent component = new KnativeComponent();
        component.setEnvironment(KnativeEnvironment.on(
            KnativeEnvironment.endpoint(Knative.EndpointKind.sink, "sink", "localhost", port)
        ));

        CamelContext context = new DefaultCamelContext();
        context.disableJMX();
        context.setStreamCaching(true);
        context.addComponent("knative", component);

        Source source = Sources.fromURI(uri);
        RoutesLoader loader = RuntimeSupport.loaderFor(context, source);
        RouteBuilder builder = loader.load(context, source);

        assertThat(loader).isInstanceOf(KnativeSourceRoutesLoader.class);
        assertThat(builder).isNotNull();

        try {
            context.addRoutes(builder);
            context.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    fromF("undertow:http://localhost:%d", port)
                        .routeId("http")
                        .to("mock:result");
                }
            });
            context.start();

            List<RouteDefinition> definitions = context.adapt(ModelCamelContext.class).getRouteDefinitions();

            assertThat(definitions).hasSize(2);
            assertThat(definitions).first().satisfies(d -> {
                assertThat(d.getOutputs()).last().hasFieldOrPropertyWithValue(
                    "endpointUri",
                    "knative://endpoint/sink"
                );
            });

            MockEndpoint mock = context.getEndpoint("mock:result", MockEndpoint.class);
            mock.expectedMessageCount(1);
            mock.expectedBodiesReceived(data);

            context.createProducerTemplate().sendBodyAndHeader(
                "direct:start",
                "",
                "MyHeader",
                data);

            mock.assertIsSatisfied();
        } finally {
            context.stop();
        }
    }

    @Test
    public void testWrapLoaderWithBeanRegistration() throws Exception {
        final int port = AvailablePortFinder.getNextAvailable();

        KnativeComponent component = new KnativeComponent();
        component.setEnvironment(KnativeEnvironment.on(
            KnativeEnvironment.endpoint(Knative.EndpointKind.source, "sink", "localhost", port)
        ));

        CamelContext context = new DefaultCamelContext();
        context.disableJMX();
        context.setStreamCaching(true);
        context.addComponent("knative", component);

        Source source = Sources.fromURI("classpath:routes.java?name=MyRoutes.java&loader=knative-source");
        RoutesLoader loader = RuntimeSupport.loaderFor(context, source);
        RouteBuilder builder = loader.load(context, source);

        assertThat(loader).isInstanceOf(KnativeSourceRoutesLoader.class);
        assertThat(builder).isNotNull();

        try {
            context.addRoutes(builder);
            context.start();

            assertThat(context.getRegistry().lookupByName("my-bean")).isInstanceOfSatisfying(String.class, "my-bean-string"::equals);
        } finally {
            context.stop();
        }
    }
}

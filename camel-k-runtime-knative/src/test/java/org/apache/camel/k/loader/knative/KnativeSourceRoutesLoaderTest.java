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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.knative.KnativeComponent;
import org.apache.camel.component.knative.KnativeConstants;
import org.apache.camel.component.knative.spi.Knative;
import org.apache.camel.component.knative.spi.KnativeEnvironment;
import org.apache.camel.component.knative.test.KnativeEnvironmentSupport;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.k.Runtime;
import org.apache.camel.k.Source;
import org.apache.camel.k.SourceLoader;
import org.apache.camel.k.Sources;
import org.apache.camel.k.http.PlatformHttpServiceContextCustomizer;
import org.apache.camel.k.support.SourcesSupport;
import org.apache.camel.k.test.AvailablePortFinder;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.RouteDefinition;
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
            Arguments.arguments("classpath:sources/routes.yaml?interceptors=knative-source"),
            Arguments.arguments("classpath:sources/routes.xml?interceptors=knative-source"),
            Arguments.arguments("classpath:sources/routes.groovy?interceptors=knative-source"),
            Arguments.arguments("classpath:sources/routes.kts?interceptors=knative-source"),
            Arguments.arguments("classpath:sources/routes.js?interceptors=knative-source"),
            Arguments.arguments("classpath:sources/routes.java?name=MyRoutes.java&interceptors=knative-source")
        ).sequential();
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testWrapLoader(String uri) throws Exception {
        LOGGER.info("uri: {}", uri);

        final String data = UUID.randomUUID().toString();
        final TestRuntime runtime = new TestRuntime();

        KnativeComponent component = new KnativeComponent();
        component.setEnvironment(KnativeEnvironment.on(
            KnativeEnvironmentSupport.endpoint(Knative.EndpointKind.sink, "sink", "localhost", runtime.port)
        ));

        CamelContext context = runtime.getCamelContext();
        context.addComponent(KnativeConstants.SCHEME, component);

        Source source = Sources.fromURI(uri);
        SourceLoader loader = SourcesSupport.load(runtime, source);

        assertThat(loader.getSupportedLanguages()).contains(source.getLanguage());
        assertThat(runtime.builders).hasSize(1);

        try {
            context.addRoutes(runtime.builders.get(0));
            context.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    fromF("platform-http:/")
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

            context.createFluentProducerTemplate()
                .to("direct:start")
                .withHeader("MyHeader", data)
                .send();

            mock.assertIsSatisfied();
        } finally {
            context.stop();
        }
    }

    static class TestRuntime implements Runtime {
        private final CamelContext camelContext;
        private final List<RoutesBuilder> builders;
        private final int port;

        public TestRuntime() {
            this.camelContext = new DefaultCamelContext();
            this.camelContext.disableJMX();
            this.camelContext.setStreamCaching(true);

            this.builders = new ArrayList<>();
            this.port = AvailablePortFinder.getNextAvailable();

            PlatformHttpServiceContextCustomizer httpService = new PlatformHttpServiceContextCustomizer();
            httpService.setBindPort(this.port);
            httpService.apply(this.camelContext);
        }

        public int getPort() {
            return port;
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

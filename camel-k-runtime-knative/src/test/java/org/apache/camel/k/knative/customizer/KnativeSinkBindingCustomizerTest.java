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
package org.apache.camel.k.knative.customizer;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.knative.KnativeComponent;
import org.apache.camel.component.knative.KnativeConstants;
import org.apache.camel.component.knative.spi.CloudEvent;
import org.apache.camel.component.knative.spi.CloudEvents;
import org.apache.camel.component.knative.spi.Knative;
import org.apache.camel.component.knative.spi.KnativeEnvironment;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.k.Runtime;
import org.apache.camel.k.Source;
import org.apache.camel.k.SourceLoader;
import org.apache.camel.k.Sources;
import org.apache.camel.k.http.PlatformHttpServiceContextCustomizer;
import org.apache.camel.k.listener.RoutesConfigurer;
import org.apache.camel.k.support.RuntimeSupport;
import org.apache.camel.k.test.AvailablePortFinder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class KnativeSinkBindingCustomizerTest {

    @Test
    public void testSinkBindingRegistration() throws Exception {
        Runtime runtime = Runtime.on(new DefaultCamelContext());
        runtime.setProperties(
                "k.sink", "http://theurl",
                "camel.k.customizer.sinkbinding.enabled", "true",
                "camel.k.customizer.sinkbinding.name", "mychannel",
                "camel.k.customizer.sinkbinding.type", "channel",
                "camel.k.customizer.sinkbinding.kind", "InMemoryChannel",
                "camel.k.customizer.sinkbinding.api-version", "messaging.knative.dev/v1beta1");


        assertThat(RuntimeSupport.configureContextCustomizers(runtime)).hasOnlyOneElementSatisfying(customizer -> {
            assertThat(customizer).isInstanceOfSatisfying(KnativeSinkBindingContextCustomizer.class, sc -> {
                assertThat(sc.getName()).isEqualTo("mychannel");
                assertThat(sc.getType()).isEqualTo(Knative.Type.channel);
                assertThat(sc.getApiVersion()).isEqualTo("messaging.knative.dev/v1beta1");
                assertThat(sc.getKind()).isEqualTo("InMemoryChannel");
            });

            var svc = runtime.getRegistry().lookupByNameAndType("mychannel", KnativeEnvironment.KnativeServiceDefinition.class);
            assertThat(svc).isNotNull();
            assertThat(svc.getUrl()).isEqualTo("http://theurl");
            assertThat(svc.getName()).isEqualTo("mychannel");
            assertThat(svc.getType()).isEqualTo(Knative.Type.channel);
            assertThat(svc.getMetadata(Knative.KNATIVE_API_VERSION)).isEqualTo("messaging.knative.dev/v1beta1");
            assertThat(svc.getMetadata(Knative.KNATIVE_KIND)).isEqualTo("InMemoryChannel");
        });
    }

    @Test
    public void testWrapLoaderWithSyntheticServiceDefinition() throws Exception {

        final String data = UUID.randomUUID().toString();
        final TestRuntime runtime = new TestRuntime();
        final String typeHeaderKey = CloudEvents.v1_0.mandatoryAttribute(CloudEvent.CAMEL_CLOUD_EVENT_TYPE).http();
        final String typeHeaderVal = UUID.randomUUID().toString();
        final String url = String.format("http://localhost:%d", runtime.port);

        KnativeComponent component = new KnativeComponent();
        component.setEnvironment(new KnativeEnvironment(Collections.emptyList()));

        Properties properties = new Properties();
        properties.put("camel.k.customizer.sinkbinding.enabled", "true");
        properties.put("camel.k.customizer.sinkbinding.name", "mySynk");
        properties.put("camel.k.customizer.sinkbinding.type", "endpoint");
        properties.put("k.sink", String.format("http://localhost:%d", runtime.port));
        properties.put("k.ce.overrides", Knative.MAPPER.writeValueAsString(Map.of(typeHeaderKey, typeHeaderVal)));

        CamelContext context = runtime.getCamelContext();
        context.getPropertiesComponent().setInitialProperties(properties);
        context.addComponent(KnativeConstants.SCHEME, component);

        RuntimeSupport.configureContextCustomizers(runtime);

        Source source = Sources.fromBytes("groovy", "from('direct:start').setBody().header('MyHeader').to('knative://endpoint/mySynk')".getBytes(StandardCharsets.UTF_8));
        SourceLoader loader = RoutesConfigurer.load(runtime, source);

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

            var services = context.getRegistry().findByType(KnativeEnvironment.KnativeServiceDefinition.class);

            assertThat(services).hasSize(1);
            assertThat(services).first().hasFieldOrPropertyWithValue("name", "mySynk");
            assertThat(services).first().hasFieldOrPropertyWithValue("url", url);

            MockEndpoint mock = context.getEndpoint("mock:result", MockEndpoint.class);
            mock.expectedMessageCount(1);
            mock.expectedBodiesReceived(data);
            mock.expectedHeaderReceived(typeHeaderKey, typeHeaderVal);

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

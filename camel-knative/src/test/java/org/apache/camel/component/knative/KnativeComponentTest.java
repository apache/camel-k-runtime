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
package org.apache.camel.component.knative;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.knative.ce.CloudEvent;
import org.apache.camel.component.knative.ce.v01.CloudEventV01;
import org.apache.camel.component.knative.ce.v02.CloudEventV02;
import org.apache.camel.component.knative.http.KnativeHttpEndpoint;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.test.AvailablePortFinder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.apache.camel.component.knative.KnativeEnvironment.mandatoryLoadFromResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

public class KnativeComponentTest {

    private CamelContext context;

    // **************************
    //
    // Setup
    //
    // **************************

    @BeforeEach
    public void before() {
        this.context = new DefaultCamelContext();
    }

    @AfterEach
    public void after() throws Exception {
        if (this.context != null) {
            this.context.stop();
        }
    }

    // **************************
    //
    // Common Tests
    //
    // **************************

    @Test
    void testLoadEnvironment() throws Exception {
        KnativeEnvironment env = mandatoryLoadFromResource(context, "classpath:/environment.json");

        assertThat(env.stream()).hasSize(3);
        assertThat(env.stream()).anyMatch(s -> s.getType() == Knative.Type.channel);
        assertThat(env.stream()).anyMatch(s -> s.getType() == Knative.Type.endpoint);

        assertThat(env.lookupService(Knative.Type.channel, "c1")).isPresent();
        assertThat(env.lookupService(Knative.Type.channel, "e1")).isNotPresent();
        assertThat(env.lookupService(Knative.Type.endpoint, "e1")).isPresent();
        assertThat(env.lookupService(Knative.Type.endpoint, "c1")).isNotPresent();

        assertThat(env.lookupServiceOrDefault(Knative.Type.endpoint, "undefined"))
            .hasFieldOrPropertyWithValue("name", "default")
            .hasFieldOrPropertyWithValue("host", "0.0.0.0")
            .hasFieldOrPropertyWithValue("port", 8080);

        assertThat(env.lookupServiceOrDefault(Knative.Type.channel, "myChannel"))
            .hasFieldOrPropertyWithValue("name", "myChannel-channel")
            .hasFieldOrPropertyWithValue("host", "")
            .hasFieldOrPropertyWithValue("port", -1);

        assertThatThrownBy(() -> env.mandatoryLookupService(Knative.Type.endpoint, "unknown"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unable to find the service \"unknown\" with type \"endpoint\"");

        assertThatThrownBy(() -> env.mandatoryLookupService(Knative.Type.channel, "unknown"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unable to find the service \"unknown\" with type \"channel\"");
    }

    @Test
    void testCreateComponent() throws Exception {
        context.start();

        assertThat(context.getComponent("knative")).isNotNull();
        assertThat(context.getComponent("knative")).isInstanceOf(KnativeComponent.class);
    }

    @Test
    void testCreateEndpoint() throws Exception {
        KnativeEnvironment env = new KnativeEnvironment(Arrays.asList(
            new KnativeEnvironment.KnativeServiceDefinition(
                Knative.Type.endpoint,
                Knative.Protocol.http,
                "myEndpoint",
                "my-node",
                9001,
                KnativeSupport.mapOf(Knative.SERVICE_META_PATH, "/a/path"))
        ));

        KnativeComponent component = context.getComponent("knative", KnativeComponent.class);
        component.setEnvironment(env);

        context.start();

        //
        // Endpoint with context path derived from service definition
        //

        KnativeEndpoint e1 = context.getEndpoint("knative:endpoint/myEndpoint", KnativeEndpoint.class);

        assertThat(e1).isNotNull();
        assertThat(e1.getService()).isNotNull();
        assertThat(e1.getService()).hasFieldOrPropertyWithValue("name", "myEndpoint");
        assertThat(e1.getService()).hasFieldOrPropertyWithValue("host", "my-node");
        assertThat(e1.getService()).hasFieldOrPropertyWithValue("port", 9001);
        assertThat(e1.getService()).hasFieldOrPropertyWithValue("type", Knative.Type.endpoint);
        assertThat(e1.getService()).hasFieldOrPropertyWithValue("protocol", Knative.Protocol.http);
        assertThat(e1.getService()).hasFieldOrPropertyWithValue("path", "/a/path");
        assertThat(e1.getEndpoint()).isInstanceOf(KnativeHttpEndpoint.class);
        assertThat(e1.getEndpoint()).hasFieldOrPropertyWithValue("endpointUri", "knative-http://my-node:9001/a/path");

        //
        // Endpoint with context path overridden by endpoint uri
        //

        KnativeEndpoint e2 = context.getEndpoint("knative:endpoint/myEndpoint/another/path", KnativeEndpoint.class);

        assertThat(e2).isNotNull();
        assertThat(e2.getService()).isNotNull();
        assertThat(e2.getService()).hasFieldOrPropertyWithValue("name", "myEndpoint");
        assertThat(e2.getService()).hasFieldOrPropertyWithValue("host", "my-node");
        assertThat(e2.getService()).hasFieldOrPropertyWithValue("port", 9001);
        assertThat(e2.getService()).hasFieldOrPropertyWithValue("type", Knative.Type.endpoint);
        assertThat(e2.getService()).hasFieldOrPropertyWithValue("protocol", Knative.Protocol.http);
        assertThat(e2.getService()).hasFieldOrPropertyWithValue("path", "/another/path");
        assertThat(e2.getEndpoint()).isInstanceOf(KnativeHttpEndpoint.class);
        assertThat(e2.getEndpoint()).hasFieldOrPropertyWithValue("endpointUri", "knative-http://my-node:9001/another/path");
    }

    @Test
    void testCreateEndpointWithComputedHost() throws Exception {
        KnativeEnvironment env = new KnativeEnvironment(Arrays.asList(
            new KnativeEnvironment.KnativeServiceDefinition(
                Knative.Type.endpoint,
                Knative.Protocol.http,
                "myEndpoint",
                "",
                -1,
                KnativeSupport.mapOf(Knative.SERVICE_META_PATH, "/a/path"))
        ));

        KnativeComponent component = context.getComponent("knative", KnativeComponent.class);
        component.setEnvironment(env);

        context.start();

        //
        // Endpoint with context path derived from service definition
        //

        KnativeEndpoint e1 = context.getEndpoint("knative:endpoint/myEndpoint", KnativeEndpoint.class);

        assertThat(e1).isNotNull();
        assertThat(e1.getService()).isNotNull();
        assertThat(e1.getService()).hasFieldOrPropertyWithValue("name", "myEndpoint");
        assertThat(e1.getService()).hasFieldOrPropertyWithValue("type", Knative.Type.endpoint);
        assertThat(e1.getService()).hasFieldOrPropertyWithValue("protocol", Knative.Protocol.http);
        assertThat(e1.getService()).hasFieldOrPropertyWithValue("path", "/a/path");
        assertThat(e1.getEndpoint()).isInstanceOf(KnativeHttpEndpoint.class);
        assertThat(e1.getEndpoint()).hasFieldOrPropertyWithValue("endpointUri", "knative-http://myEndpoint:80/a/path");
    }

    @Test
    void testCreateEndpointWithComputedHostAndNamespace() throws Exception {
        KnativeEnvironment env = new KnativeEnvironment(Arrays.asList(
            new KnativeEnvironment.KnativeServiceDefinition(
                Knative.Type.endpoint,
                Knative.Protocol.http,
                "myEndpoint",
                "",
                -1,
                KnativeSupport.mapOf(
                    Knative.SERVICE_META_PATH, "/a/path",
                        Knative.SERVICE_META_ZONE, "myNamespace"))
        ));

        KnativeComponent component = context.getComponent("knative", KnativeComponent.class);
        component.setEnvironment(env);

        context.start();

        //
        // Endpoint with context path derived from service definition
        //

        KnativeEndpoint e1 = context.getEndpoint("knative:endpoint/myEndpoint", KnativeEndpoint.class);

        assertThat(e1).isNotNull();
        assertThat(e1.getService()).isNotNull();
        assertThat(e1.getService()).hasFieldOrPropertyWithValue("name", "myEndpoint");
        assertThat(e1.getService()).hasFieldOrPropertyWithValue("type", Knative.Type.endpoint);
        assertThat(e1.getService()).hasFieldOrPropertyWithValue("protocol", Knative.Protocol.http);
        assertThat(e1.getService()).hasFieldOrPropertyWithValue("path", "/a/path");
        assertThat(e1.getEndpoint()).isInstanceOf(KnativeHttpEndpoint.class);
        assertThat(e1.getEndpoint()).hasFieldOrPropertyWithValue("endpointUri", "knative-http://myEndpoint.myNamespace:80/a/path");
    }

    @Test
    void testCreateEndpointWithComputedHostAndNamespaceWithProperty() throws Exception {
        KnativeEnvironment env = new KnativeEnvironment(Arrays.asList(
            new KnativeEnvironment.KnativeServiceDefinition(
                Knative.Type.endpoint,
                Knative.Protocol.http,
                "myEndpoint",
                "",
                -1,
                KnativeSupport.mapOf(
                    Knative.SERVICE_META_PATH, "/a/path",
                    Knative.SERVICE_META_ZONE, "{{myNamespaceKey}}"))
        ));

        Properties properties = new Properties();
        properties.setProperty("myNamespaceKey", "myNamespace");

        PropertiesComponent pc = context.getComponent("properties", PropertiesComponent.class);
        pc.setInitialProperties(properties);

        KnativeComponent component = context.getComponent("knative", KnativeComponent.class);
        component.setEnvironment(env);

        context.start();

        //
        // Endpoint with context path derived from service definition
        //

        KnativeEndpoint e1 = context.getEndpoint("knative:endpoint/myEndpoint", KnativeEndpoint.class);

        assertThat(e1).isNotNull();
        assertThat(e1.getService()).isNotNull();
        assertThat(e1.getService()).hasFieldOrPropertyWithValue("name", "myEndpoint");
        assertThat(e1.getService()).hasFieldOrPropertyWithValue("type", Knative.Type.endpoint);
        assertThat(e1.getService()).hasFieldOrPropertyWithValue("protocol", Knative.Protocol.http);
        assertThat(e1.getService()).hasFieldOrPropertyWithValue("path", "/a/path");
        assertThat(e1.getEndpoint()).isInstanceOf(KnativeHttpEndpoint.class);
        assertThat(e1.getEndpoint()).hasFieldOrPropertyWithValue("endpointUri", "knative-http://myEndpoint.myNamespace:80/a/path");
    }

    @Test
    void testCreateEndpointWithDefaults() throws Exception {
        KnativeEnvironment env = new KnativeEnvironment(Collections.emptyList());

        KnativeComponent component = context.getComponent("knative", KnativeComponent.class);
        component.setEnvironment(env);

        context.start();

        //
        // Endpoint with context path derived from service definition
        //

        KnativeEndpoint e1 = context.getEndpoint("knative:endpoint/myEndpoint/my/path", KnativeEndpoint.class);

        assertThat(e1).isNotNull();
        assertThat(e1.getService()).isNotNull();
        assertThat(e1.getService()).hasFieldOrPropertyWithValue("name", "myEndpoint");
        assertThat(e1.getService()).hasFieldOrPropertyWithValue("type", Knative.Type.endpoint);
        assertThat(e1.getService()).hasFieldOrPropertyWithValue("protocol", Knative.Protocol.http);
        assertThat(e1.getService()).hasFieldOrPropertyWithValue("path", "/my/path");
        assertThat(e1.getEndpoint()).isInstanceOf(KnativeHttpEndpoint.class);
        assertThat(e1.getEndpoint()).hasFieldOrPropertyWithValue("endpointUri", "knative-http://myEndpoint:80/my/path");

        //
        // Endpoint with context path overridden by endpoint uri
        //

        KnativeEndpoint e2 = context.getEndpoint("knative:channel/myChannel/another/path", KnativeEndpoint.class);

        assertThat(e2).isNotNull();
        assertThat(e2.getService()).isNotNull();
        assertThat(e2.getService()).hasFieldOrPropertyWithValue("name", "myChannel-channel");
        assertThat(e2.getService()).hasFieldOrPropertyWithValue("type", Knative.Type.channel);
        assertThat(e2.getService()).hasFieldOrPropertyWithValue("protocol", Knative.Protocol.http);
        assertThat(e2.getService()).hasFieldOrPropertyWithValue("path", "/another/path");
        assertThat(e2.getEndpoint()).isInstanceOf(KnativeHttpEndpoint.class);
        assertThat(e2.getEndpoint()).hasFieldOrPropertyWithValue("endpointUri", "knative-http://myChannel-channel:80/another/path");
    }

    @Test
    void testCreateEventEndpoint() throws Exception {
        KnativeEnvironment env = new KnativeEnvironment(Arrays.asList(
            new KnativeEnvironment.KnativeServiceDefinition(
                Knative.Type.event,
                Knative.Protocol.http,
                "default",
                "localhost",
                9001,
                Collections.emptyMap())
        ));

        KnativeComponent component = context.getComponent("knative", KnativeComponent.class);
        component.setEnvironment(env);

        context.start();

        KnativeEndpoint e1 = context.getEndpoint("knative:event/myEventType", KnativeEndpoint.class);

        assertThat(e1).isNotNull();
        assertThat(e1.getService()).isNotNull();
        assertThat(e1.getService()).hasFieldOrPropertyWithValue("name", "default");
        assertThat(e1.getService()).hasFieldOrPropertyWithValue("host", "localhost");
        assertThat(e1.getService()).hasFieldOrPropertyWithValue("port", 9001);
        assertThat(e1.getService()).hasFieldOrPropertyWithValue("type", Knative.Type.event);
        assertThat(e1.getService()).hasFieldOrPropertyWithValue("protocol", Knative.Protocol.http);
        assertThat(e1.getEndpoint()).isInstanceOfSatisfying(KnativeHttpEndpoint.class, endpoint -> {
            assertThat(endpoint.getEndpointUri()).isEqualTo("knative-http://localhost:9001?filter.ce-type=myEventType");
        });

        KnativeEndpoint e2 = context.getEndpoint("knative:event/myEventType?filter.ceAuthorName=Someone", KnativeEndpoint.class);

        assertThat(e2).isNotNull();
        assertThat(e2.getService()).isNotNull();
        assertThat(e2.getService()).hasFieldOrPropertyWithValue("name", "default");
        assertThat(e2.getService()).hasFieldOrPropertyWithValue("host", "localhost");
        assertThat(e2.getService()).hasFieldOrPropertyWithValue("port", 9001);
        assertThat(e2.getService()).hasFieldOrPropertyWithValue("type", Knative.Type.event);
        assertThat(e2.getService()).hasFieldOrPropertyWithValue("protocol", Knative.Protocol.http);
        assertThat(e2.getEndpoint()).isInstanceOfSatisfying(KnativeHttpEndpoint.class, endpoint -> {
            assertThat(endpoint.getEndpointUri()).isEqualTo("knative-http://localhost:9001?filter.ce-type=myEventType&filter.ceAuthorName=Someone");
        });
    }

    @Test
    void testFailForUnknownProperties() throws Exception {
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("knative:endpoint/from?aproperty=test")
                    .log("${body}");
            }
        });

        assertThrows(Exception.class, context::start);
    }

    @Test
    void testDoNotFailForKnownButUnusedProperties() throws Exception {
        KnativeEnvironment env = KnativeEnvironment.on(
            new KnativeEnvironment.KnativeServiceDefinition(
                Knative.Type.endpoint,
                Knative.Protocol.http,
                "from",
                "localhost",
                9001,
                Collections.emptyMap())
        );

        KnativeComponent component = context.getComponent("knative", KnativeComponent.class);
        component.setEnvironment(env);

        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("knative:endpoint/from?kind=InMemoryChannel&apiVersion=messaging.knative.dev/v1")
                    .log("${body}");
            }
        });

        try {
            context.start();
        } catch (Exception e) {
            fail("Should not reach this point", e);
        }
    }

    // ***************************************
    //
    // Cloud Events Tests
    //
    // ***************************************

    private static Stream<Arguments> provideCloudEventsImplementations() {
        return Stream.of(
            Arguments.of(new CloudEventV01()),
            Arguments.of(new CloudEventV02())
        );
    }

    @ParameterizedTest
    @MethodSource("provideCloudEventsImplementations")
    void testInvokeEndpoint(CloudEvent ce) throws Exception {
        final int port = AvailablePortFinder.getNextAvailable();

        KnativeEnvironment env = new KnativeEnvironment(Arrays.asList(
            new KnativeEnvironment.KnativeServiceDefinition(
                Knative.Type.endpoint,
                Knative.Protocol.http,
                "myEndpoint",
                "localhost",
                port,
                KnativeSupport.mapOf(
                    Knative.SERVICE_META_PATH, "/a/path",
                    Knative.KNATIVE_EVENT_TYPE, "org.apache.camel.event",
                    Knative.CONTENT_TYPE, "text/plain"
                ))
        ));

        KnativeComponent component = context.getComponent("knative", KnativeComponent.class);
        component.setCloudEventsSpecVersion(ce.version());
        component.setEnvironment(env);

        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:source")
                    .to("knative:endpoint/myEndpoint");
                fromF("undertow:http://localhost:%d/a/path", port)
                    .to("mock:ce");
            }
        });

        context.start();

        MockEndpoint mock = context.getEndpoint("mock:ce", MockEndpoint.class);
        mock.expectedHeaderReceived(ce.attributes().spec(), ce.version());
        mock.expectedHeaderReceived(ce.attributes().type(), "org.apache.camel.event");
        mock.expectedHeaderReceived(ce.attributes().source(), "knative://endpoint/myEndpoint");
        mock.expectedHeaderReceived(Exchange.CONTENT_TYPE, "text/plain");
        mock.expectedMessagesMatches(e -> e.getIn().getHeaders().containsKey(ce.attributes().time()));
        mock.expectedMessagesMatches(e -> e.getIn().getHeaders().containsKey(ce.attributes().id()));
        mock.expectedBodiesReceived("test");
        mock.expectedMessageCount(1);

        context.createProducerTemplate().sendBody("direct:source", "test");

        mock.assertIsSatisfied();
    }

    @ParameterizedTest
    @MethodSource("provideCloudEventsImplementations")
    void testConsumeStructuredContent(CloudEvent ce) throws Exception {
        final int port = AvailablePortFinder.getNextAvailable();

        KnativeEnvironment env = new KnativeEnvironment(Arrays.asList(
            new KnativeEnvironment.KnativeServiceDefinition(
                Knative.Type.endpoint,
                Knative.Protocol.http,
                "myEndpoint",
                "localhost",
                port,
                KnativeSupport.mapOf(
                    Knative.SERVICE_META_PATH, "/a/path",
                    Knative.KNATIVE_EVENT_TYPE, "org.apache.camel.event",
                    Knative.CONTENT_TYPE, "text/plain"
                ))
        ));

        KnativeComponent component = context.getComponent("knative", KnativeComponent.class);
        component.setCloudEventsSpecVersion(ce.version());
        component.setEnvironment(env);

        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("knative:endpoint/myEndpoint")
                    .to("mock:ce");
                from("direct:source")
                    .toF("undertow:http://localhost:%d/a/path", port);
            }
        });

        context.start();

        MockEndpoint mock = context.getEndpoint("mock:ce", MockEndpoint.class);
        mock.expectedHeaderReceived(ce.attributes().spec(), ce.version());
        mock.expectedHeaderReceived(ce.attributes().type(), "org.apache.camel.event");
        mock.expectedHeaderReceived(ce.attributes().id(), "myEventID");
        mock.expectedHeaderReceived(ce.attributes().source(), "/somewhere");
        mock.expectedHeaderReceived(Exchange.CONTENT_TYPE, "text/plain");
        mock.expectedMessagesMatches(e -> e.getIn().getHeaders().containsKey(ce.attributes().time()));
        mock.expectedBodiesReceived("test");
        mock.expectedMessageCount(1);

        context.createProducerTemplate().send(
            "direct:source",
            e -> {
                e.getIn().setHeader(Exchange.CONTENT_TYPE, Knative.MIME_STRUCTURED_CONTENT_MODE);

                if (Objects.equals(ce.version(), ce.version())) {
                    e.getIn().setBody(new ObjectMapper().writeValueAsString(KnativeSupport.mapOf(
                        "cloudEventsVersion", ce.version(),
                        "eventType", "org.apache.camel.event",
                        "eventID", "myEventID",
                        "eventTime", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now()),
                        "source", "/somewhere",
                        "contentType", "text/plain",
                        "data", "test"
                    )));
                }
                if (Objects.equals(CloudEventV02.VERSION, ce.version())) {
                    e.getIn().setBody(new ObjectMapper().writeValueAsString(KnativeSupport.mapOf(
                        "specversion", ce.version(),
                        "type", "org.apache.camel.event",
                        "id", "myEventID",
                        "time", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now()),
                        "source", "/somewhere",
                        "contenttype", "text/plain",
                        "data", "test"
                    )));
                }
            }
        );

        mock.assertIsSatisfied();
    }

    @ParameterizedTest
    @MethodSource("provideCloudEventsImplementations")
    void testConsumeContent(CloudEvent ce) throws Exception {
        final int port = AvailablePortFinder.getNextAvailable();

        KnativeEnvironment env = new KnativeEnvironment(Arrays.asList(
            new KnativeEnvironment.KnativeServiceDefinition(
                Knative.Type.endpoint,
                Knative.Protocol.http,
                "myEndpoint",
                "localhost",
                port,
                KnativeSupport.mapOf(
                    Knative.SERVICE_META_PATH, "/a/path",
                    Knative.KNATIVE_EVENT_TYPE, "org.apache.camel.event",
                    Knative.CONTENT_TYPE, "text/plain"
                ))
        ));

        KnativeComponent component = context.getComponent("knative", KnativeComponent.class);
        component.setCloudEventsSpecVersion(ce.version());
        component.setEnvironment(env);

        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("knative:endpoint/myEndpoint")
                    .to("mock:ce");

                from("direct:source")
                    .toF("undertow:http://localhost:%d/a/path", port);
            }
        });

        context.start();

        MockEndpoint mock = context.getEndpoint("mock:ce", MockEndpoint.class);
        mock.expectedHeaderReceived(ce.attributes().spec(), ce.version());
        mock.expectedHeaderReceived(ce.attributes().type(), "org.apache.camel.event");
        mock.expectedHeaderReceived(ce.attributes().id(), "myEventID");
        mock.expectedHeaderReceived(ce.attributes().source(), "/somewhere");
        mock.expectedHeaderReceived(Exchange.CONTENT_TYPE, "text/plain");
        mock.expectedMessagesMatches(e -> e.getIn().getHeaders().containsKey(ce.attributes().time()));
        mock.expectedBodiesReceived("test");
        mock.expectedMessageCount(1);

        context.createProducerTemplate().send(
            "direct:source",
            e -> {
                e.getIn().setHeader(Exchange.CONTENT_TYPE, "text/plain");
                e.getIn().setHeader(ce.attributes().spec(), ce.version());
                e.getIn().setHeader(ce.attributes().type(), "org.apache.camel.event");
                e.getIn().setHeader(ce.attributes().id(), "myEventID");
                e.getIn().setHeader(ce.attributes().time(), DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now()));
                e.getIn().setHeader(ce.attributes().source(), "/somewhere");
                e.getIn().setBody("test");
            }
        );

        mock.assertIsSatisfied();
    }

    @ParameterizedTest
    @MethodSource("provideCloudEventsImplementations")
    void testConsumeContentWithFilter(CloudEvent ce) throws Exception {
        final int port = AvailablePortFinder.getNextAvailable();

        KnativeEnvironment env = new KnativeEnvironment(Arrays.asList(
            new KnativeEnvironment.KnativeServiceDefinition(
                Knative.Type.endpoint,
                Knative.Protocol.http,
                "ep1",
                "localhost",
                port,
                KnativeSupport.mapOf(
                    Knative.KNATIVE_EVENT_TYPE, "org.apache.camel.event",
                    Knative.CONTENT_TYPE, "text/plain",
                    Knative.FILTER_HEADER_NAME, ce.attributes().source(),
                    Knative.FILTER_HEADER_VALUE, "CE1"
                )),
            new KnativeEnvironment.KnativeServiceDefinition(
                Knative.Type.endpoint,
                Knative.Protocol.http,
                "ep2",
                "localhost",
                port,
                KnativeSupport.mapOf(
                    Knative.KNATIVE_EVENT_TYPE, "org.apache.camel.event",
                    Knative.CONTENT_TYPE, "text/plain",
                    Knative.FILTER_HEADER_NAME, ce.attributes().source(),
                    Knative.FILTER_HEADER_VALUE, "CE2"
                ))
        ));

        KnativeComponent component = context.getComponent("knative", KnativeComponent.class);
        component.setCloudEventsSpecVersion(ce.version());
        component.setEnvironment(env);

        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("knative:endpoint/ep1")
                    .convertBodyTo(String.class)
                    .to("log:ce1?showAll=true&multiline=true")
                    .to("mock:ce1");
                from("knative:endpoint/ep2")
                    .convertBodyTo(String.class)
                    .to("log:ce2?showAll=true&multiline=true")
                    .to("mock:ce2");

                from("direct:source")
                    .setBody()
                        .constant("test")
                    .setHeader(Exchange.HTTP_METHOD)
                        .constant("POST")
                    .setHeader(Exchange.HTTP_QUERY)
                        .simple("filter." + ce.attributes().source() + "=${header.FilterVal}")
                    .toD("undertow:http://localhost:" + port);
            }
        });

        context.start();

        MockEndpoint mock1 = context.getEndpoint("mock:ce1", MockEndpoint.class);
        mock1.expectedMessagesMatches(e -> e.getIn().getHeaders().containsKey(ce.attributes().time()));
        mock1.expectedHeaderReceived(ce.attributes().spec(), ce.version());
        mock1.expectedHeaderReceived(ce.attributes().type(), "org.apache.camel.event");
        mock1.expectedHeaderReceived(ce.attributes().id(), "myEventID1");
        mock1.expectedHeaderReceived(ce.attributes().source(), "CE1");
        mock1.expectedBodiesReceived("test");
        mock1.expectedMessageCount(1);

        MockEndpoint mock2 = context.getEndpoint("mock:ce2", MockEndpoint.class);
        mock2.expectedMessagesMatches(e -> e.getIn().getHeaders().containsKey(ce.attributes().time()));
        mock2.expectedHeaderReceived(ce.attributes().spec(), ce.version());
        mock2.expectedHeaderReceived(ce.attributes().type(), "org.apache.camel.event");
        mock2.expectedHeaderReceived(ce.attributes().id(), "myEventID2");
        mock2.expectedHeaderReceived(ce.attributes().source(), "CE2");
        mock2.expectedBodiesReceived("test");
        mock2.expectedMessageCount(1);

        context.createProducerTemplate().send(
            "direct:source",
            e -> {
                e.getIn().setHeader("FilterVal", "CE1");
                e.getIn().setHeader(ce.attributes().spec(), ce.version());
                e.getIn().setHeader(ce.attributes().type(), "org.apache.camel.event");
                e.getIn().setHeader(ce.attributes().id(), "myEventID1");
                e.getIn().setHeader(ce.attributes().time(), DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now()));
                e.getIn().setHeader(ce.attributes().source(), "CE1");
            }
        );
        context.createProducerTemplate().send(
            "direct:source",
            e -> {
                e.getIn().setHeader("FilterVal", "CE2");
                e.getIn().setHeader(ce.attributes().spec(), ce.version());
                e.getIn().setHeader(ce.attributes().type(), "org.apache.camel.event");
                e.getIn().setHeader(ce.attributes().id(), "myEventID2");
                e.getIn().setHeader(ce.attributes().time(), DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now()));
                e.getIn().setHeader(ce.attributes().source(), "CE2");
            }
        );

        mock1.assertIsSatisfied();
        mock2.assertIsSatisfied();
    }



    @ParameterizedTest
    @MethodSource("provideCloudEventsImplementations")
    void testConsumeEventContent(CloudEvent ce) throws Exception {
        final int port = AvailablePortFinder.getNextAvailable();

        KnativeEnvironment env = KnativeEnvironment.on(
            new KnativeEnvironment.KnativeServiceDefinition(
                Knative.Type.event,
                Knative.Protocol.http,
                "default",
                "localhost",
                port,
                Collections.emptyMap())
        );

        KnativeComponent component = context.getComponent("knative", KnativeComponent.class);
        component.setCloudEventsSpecVersion(ce.version());
        component.setEnvironment(env);

        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("knative:event/event1")
                    .convertBodyTo(String.class)
                    .to("log:ce1?showAll=true&multiline=true")
                    .to("mock:ce1");
                from("knative:event/event2")
                    .convertBodyTo(String.class)
                    .to("log:ce2?showAll=true&multiline=true")
                    .to("mock:ce2");

                from("direct:source")
                    .setBody()
                    .constant("test")
                    .setHeader(Exchange.HTTP_METHOD)
                        .constant("POST")
                    .toD("undertow:http://localhost:" + port);
            }
        });

        context.start();

        MockEndpoint mock1 = context.getEndpoint("mock:ce1", MockEndpoint.class);
        mock1.expectedMessagesMatches(e -> e.getIn().getHeaders().containsKey(ce.attributes().time()));
        mock1.expectedHeaderReceived(ce.attributes().spec(), ce.version());
        mock1.expectedHeaderReceived(ce.attributes().type(), "event1");
        mock1.expectedHeaderReceived(ce.attributes().id(), "myEventID1");
        mock1.expectedHeaderReceived(ce.attributes().source(), "CE1");
        mock1.expectedBodiesReceived("test");
        mock1.expectedMessageCount(1);

        MockEndpoint mock2 = context.getEndpoint("mock:ce2", MockEndpoint.class);
        mock2.expectedMessagesMatches(e -> e.getIn().getHeaders().containsKey(ce.attributes().time()));
        mock2.expectedHeaderReceived(ce.attributes().spec(), ce.version());
        mock2.expectedHeaderReceived(ce.attributes().type(), "event2");
        mock2.expectedHeaderReceived(ce.attributes().id(), "myEventID2");
        mock2.expectedHeaderReceived(ce.attributes().source(), "CE2");
        mock2.expectedBodiesReceived("test");
        mock2.expectedMessageCount(1);

        context.createProducerTemplate().send(
            "direct:source",
            e -> {
                e.getIn().setHeader("FilterVal", "CE1");
                e.getIn().setHeader(ce.attributes().spec(), ce.version());
                e.getIn().setHeader(ce.attributes().type(), "event1");
                e.getIn().setHeader(ce.attributes().id(), "myEventID1");
                e.getIn().setHeader(ce.attributes().time(), DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now()));
                e.getIn().setHeader(ce.attributes().source(), "CE1");
            }
        );
        context.createProducerTemplate().send(
            "direct:source",
            e -> {
                e.getIn().setHeader("FilterVal", "CE2");
                e.getIn().setHeader(ce.attributes().spec(), ce.version());
                e.getIn().setHeader(ce.attributes().type(), "event2");
                e.getIn().setHeader(ce.attributes().id(), "myEventID2");
                e.getIn().setHeader(ce.attributes().time(), DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now()));
                e.getIn().setHeader(ce.attributes().source(), "CE2");
            }
        );

        mock1.assertIsSatisfied();
        mock2.assertIsSatisfied();
    }

    @ParameterizedTest
    @MethodSource("provideCloudEventsImplementations")
    void testReply(CloudEvent ce) throws Exception {
        final int port = AvailablePortFinder.getNextAvailable();

        KnativeEnvironment env = new KnativeEnvironment(Arrays.asList(
            new KnativeEnvironment.KnativeServiceDefinition(
                Knative.Type.endpoint,
                Knative.Protocol.http,
                "from",
                "localhost",
                port,
                KnativeSupport.mapOf(
                    Knative.KNATIVE_EVENT_TYPE, "org.apache.camel.event",
                    Knative.CONTENT_TYPE, "text/plain"
                )),
            new KnativeEnvironment.KnativeServiceDefinition(
                Knative.Type.endpoint,
                Knative.Protocol.http,
                "to",
                "localhost",
                port,
                KnativeSupport.mapOf(
                    Knative.KNATIVE_EVENT_TYPE, "org.apache.camel.event",
                    Knative.CONTENT_TYPE, "text/plain"
                ))
            )
        );

        KnativeComponent component = context.getComponent("knative", KnativeComponent.class);
        component.setCloudEventsSpecVersion(ce.version());
        component.setEnvironment(env);

        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("knative:endpoint/from")
                    .convertBodyTo(String.class)
                    .setBody().constant("consumer");
                from("direct:source")
                    .to("knative://endpoint/to")
                    .log("${body}")
                    .to("mock:to");
            }
        });

        MockEndpoint mock = context.getEndpoint("mock:to", MockEndpoint.class);
        mock.expectedBodiesReceived("consumer");
        mock.expectedMessageCount(1);

        context.start();
        context.createProducerTemplate().sendBody("direct:source", "");

        mock.assertIsSatisfied();
    }
}

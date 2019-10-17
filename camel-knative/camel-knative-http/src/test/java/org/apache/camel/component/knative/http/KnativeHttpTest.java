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
package org.apache.camel.component.knative.http;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.Undertow;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelException;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.knative.KnativeComponent;
import org.apache.camel.component.knative.spi.CloudEvent;
import org.apache.camel.component.knative.spi.CloudEvents;
import org.apache.camel.component.knative.spi.Knative;
import org.apache.camel.component.knative.spi.KnativeEnvironment;
import org.apache.camel.component.knative.spi.KnativeSupport;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.http.common.HttpOperationFailedException;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.test.AvailablePortFinder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.apache.camel.component.knative.http.KnativeHttpTestSupport.configureKnativeComponent;
import static org.apache.camel.component.knative.spi.KnativeEnvironment.channel;
import static org.apache.camel.component.knative.spi.KnativeEnvironment.endpoint;
import static org.apache.camel.component.knative.spi.KnativeEnvironment.event;
import static org.assertj.core.api.Assertions.assertThat;

public class KnativeHttpTest {

    private CamelContext context;
    private ProducerTemplate template;
    private int port;

    // **************************
    //
    // Setup
    //
    // **************************

    @BeforeEach
    public void before() {
        this.context = new DefaultCamelContext();
        this.template = this.context.createProducerTemplate();
        this.port = AvailablePortFinder.getNextAvailable();
    }

    @AfterEach
    public void after() {
        if (this.context != null) {
            this.context.stop();
        }
    }

    // **************************
    //
    // Tests
    //
    // **************************

    @Test
    void testCreateComponent() {
        context.start();

        assertThat(context.getComponent("knative")).isInstanceOfSatisfying(KnativeComponent.class, c -> {
            assertThat(c.getTransport()).isInstanceOf(KnativeHttpTransport.class);
        });

    }

    private static Stream<Arguments> provideCloudEventsImplementations() {
        return Stream.of(
            Arguments.of(CloudEvents.V01),
            Arguments.of(CloudEvents.V02),
            Arguments.of(CloudEvents.V03)
        );
    }

    @ParameterizedTest
    @MethodSource("provideCloudEventsImplementations")
    void testInvokeEndpoint(CloudEvent ce) throws Exception {
        configureKnativeComponent(
            context,
            ce,
            endpoint(
                Knative.EndpointKind.sink,
                "myEndpoint",
                "localhost",
                port,
                KnativeSupport.mapOf(
                    Knative.SERVICE_META_PATH, "/a/path",
                    Knative.KNATIVE_EVENT_TYPE, "org.apache.camel.event",
                    Knative.CONTENT_TYPE, "text/plain"
                ))
        );

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
        mock.expectedHeaderReceived(ce.mandatoryAttribute("version").id(), ce.version());
        mock.expectedHeaderReceived(ce.mandatoryAttribute("type").id(), "org.apache.camel.event");
        mock.expectedHeaderReceived(ce.mandatoryAttribute("source").id(), "knative://endpoint/myEndpoint");
        mock.expectedHeaderReceived(Exchange.CONTENT_TYPE, "text/plain");
        mock.expectedMessagesMatches(e -> e.getMessage().getHeaders().containsKey(ce.mandatoryAttribute("time").id()));
        mock.expectedMessagesMatches(e -> e.getMessage().getHeaders().containsKey(ce.mandatoryAttribute("id").id()));
        mock.expectedBodiesReceived("test");
        mock.expectedMessageCount(1);

        context.createProducerTemplate().sendBody("direct:source", "test");

        mock.assertIsSatisfied();
    }

    @ParameterizedTest
    @MethodSource("provideCloudEventsImplementations")
    void testConsumeStructuredContent(CloudEvent ce) throws Exception {
        configureKnativeComponent(
            context,
            ce,
            endpoint(
                Knative.EndpointKind.source,
                "myEndpoint",
                "localhost",
                port,
                KnativeSupport.mapOf(
                    Knative.SERVICE_META_PATH, "/a/path",
                    Knative.KNATIVE_EVENT_TYPE, "org.apache.camel.event",
                    Knative.CONTENT_TYPE, "text/plain"
                ))
        );

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
        mock.expectedHeaderReceived(ce.mandatoryAttribute("version").id(), ce.version());
        mock.expectedHeaderReceived(ce.mandatoryAttribute("type").id(), "org.apache.camel.event");
        mock.expectedHeaderReceived(ce.mandatoryAttribute("id").id(), "myEventID");
        mock.expectedHeaderReceived(ce.mandatoryAttribute("source").id(), "/somewhere");
        mock.expectedHeaderReceived(Exchange.CONTENT_TYPE, "text/plain");
        mock.expectedMessagesMatches(e -> e.getMessage().getHeaders().containsKey(ce.mandatoryAttribute("time").id()));
        mock.expectedBodiesReceived("test");
        mock.expectedMessageCount(1);

        context.createProducerTemplate().send(
            "direct:source",
            e -> {
                e.getMessage().setHeader(Exchange.CONTENT_TYPE, Knative.MIME_STRUCTURED_CONTENT_MODE);

                if (Objects.equals(CloudEvents.V01.version(), ce.version())) {
                    e.getMessage().setBody(new ObjectMapper().writeValueAsString(KnativeSupport.mapOf(
                        "cloudEventsVersion", ce.version(),
                        "eventType", "org.apache.camel.event",
                        "eventID", "myEventID",
                        "eventTime", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now()),
                        "source", "/somewhere",
                        "contentType", "text/plain",
                        "data", "test"
                    )));
                } else if (Objects.equals(CloudEvents.V02.version(), ce.version())) {
                    e.getMessage().setBody(new ObjectMapper().writeValueAsString(KnativeSupport.mapOf(
                        "specversion", ce.version(),
                        "type", "org.apache.camel.event",
                        "id", "myEventID",
                        "time", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now()),
                        "source", "/somewhere",
                        "contenttype", "text/plain",
                        "data", "test"
                    )));
                } else if (Objects.equals(CloudEvents.V03.version(), ce.version())) {
                    e.getMessage().setBody(new ObjectMapper().writeValueAsString(KnativeSupport.mapOf(
                        "specversion", ce.version(),
                        "type", "org.apache.camel.event",
                        "id", "myEventID",
                        "time", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now()),
                        "source", "/somewhere",
                        "datacontenttype", "text/plain",
                        "data", "test"
                    )));
                } else {
                    throw new IllegalArgumentException("Unknown CloudEvent spec: " + ce.version());
                }
            }
        );

        mock.assertIsSatisfied();
    }

    @ParameterizedTest
    @MethodSource("provideCloudEventsImplementations")
    void testConsumeContent(CloudEvent ce) throws Exception {
        configureKnativeComponent(
            context,
            ce,
            endpoint(
                Knative.EndpointKind.source,
                "myEndpoint",
                "localhost",
                port,
                KnativeSupport.mapOf(
                    Knative.SERVICE_META_PATH, "/a/path",
                    Knative.KNATIVE_EVENT_TYPE, "org.apache.camel.event",
                    Knative.CONTENT_TYPE, "text/plain"
                ))
        );

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
        mock.expectedHeaderReceived(ce.mandatoryAttribute("version").id(), ce.version());
        mock.expectedHeaderReceived(ce.mandatoryAttribute("type").id(), "org.apache.camel.event");
        mock.expectedHeaderReceived(ce.mandatoryAttribute("id").id(), "myEventID");
        mock.expectedHeaderReceived(ce.mandatoryAttribute("source").id(), "/somewhere");
        mock.expectedHeaderReceived(Exchange.CONTENT_TYPE, "text/plain");
        mock.expectedMessagesMatches(e -> e.getMessage().getHeaders().containsKey(ce.mandatoryAttribute("time").id()));
        mock.expectedBodiesReceived("test");
        mock.expectedMessageCount(1);

        context.createProducerTemplate().send(
            "direct:source",
            e -> {
                e.getMessage().setHeader(Exchange.CONTENT_TYPE, "text/plain");
                e.getMessage().setHeader(ce.mandatoryAttribute("version").id(), ce.version());
                e.getMessage().setHeader(ce.mandatoryAttribute("type").id(), "org.apache.camel.event");
                e.getMessage().setHeader(ce.mandatoryAttribute("id").id(), "myEventID");
                e.getMessage().setHeader(ce.mandatoryAttribute("time").id(), DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now()));
                e.getMessage().setHeader(ce.mandatoryAttribute("source").id(), "/somewhere");
                e.getMessage().setBody("test");
            }
        );

        mock.assertIsSatisfied();
    }

    @ParameterizedTest
    @MethodSource("provideCloudEventsImplementations")
    void testConsumeContentWithFilter(CloudEvent ce) throws Exception {
        configureKnativeComponent(
            context,
            ce,
            endpoint(
                Knative.EndpointKind.source,
                "ep1",
                "localhost",
                port,
                KnativeSupport.mapOf(
                    Knative.KNATIVE_EVENT_TYPE, "org.apache.camel.event",
                    Knative.CONTENT_TYPE, "text/plain",
                    Knative.KNATIVE_FILTER_PREFIX + ce.mandatoryAttribute("source").id(), "CE1"
                )),
            endpoint(
                Knative.EndpointKind.source,
                "ep2",
                "localhost",
                port,
                KnativeSupport.mapOf(
                    Knative.KNATIVE_EVENT_TYPE, "org.apache.camel.event",
                    Knative.CONTENT_TYPE, "text/plain",
                    Knative.KNATIVE_FILTER_PREFIX + ce.mandatoryAttribute("source").id(), "CE2"
                ))
        );

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
                    .toD("undertow:http://localhost:" + port);
            }
        });

        context.start();

        MockEndpoint mock1 = context.getEndpoint("mock:ce1", MockEndpoint.class);
        mock1.expectedMessagesMatches(e -> e.getMessage().getHeaders().containsKey(ce.mandatoryAttribute("time").id()));
        mock1.expectedHeaderReceived(ce.mandatoryAttribute("version").id(), ce.version());
        mock1.expectedHeaderReceived(ce.mandatoryAttribute("type").id(), "org.apache.camel.event");
        mock1.expectedHeaderReceived(ce.mandatoryAttribute("id").id(), "myEventID1");
        mock1.expectedHeaderReceived(ce.mandatoryAttribute("source").id(), "CE1");
        mock1.expectedBodiesReceived("test");
        mock1.expectedMessageCount(1);

        MockEndpoint mock2 = context.getEndpoint("mock:ce2", MockEndpoint.class);
        mock2.expectedMessagesMatches(e -> e.getMessage().getHeaders().containsKey(ce.mandatoryAttribute("time").id()));
        mock2.expectedHeaderReceived(ce.mandatoryAttribute("version").id(), ce.version());
        mock2.expectedHeaderReceived(ce.mandatoryAttribute("type").id(), "org.apache.camel.event");
        mock2.expectedHeaderReceived(ce.mandatoryAttribute("id").id(), "myEventID2");
        mock2.expectedHeaderReceived(ce.mandatoryAttribute("source").id(), "CE2");
        mock2.expectedBodiesReceived("test");
        mock2.expectedMessageCount(1);

        context.createProducerTemplate().send(
            "direct:source",
            e -> {
                e.getMessage().setHeader(ce.mandatoryAttribute("version").id(), ce.version());
                e.getMessage().setHeader(ce.mandatoryAttribute("type").id(), "org.apache.camel.event");
                e.getMessage().setHeader(ce.mandatoryAttribute("id").id(), "myEventID1");
                e.getMessage().setHeader(ce.mandatoryAttribute("time").id(), DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now()));
                e.getMessage().setHeader(ce.mandatoryAttribute("source").id(), "CE1");
            }
        );
        context.createProducerTemplate().send(
            "direct:source",
            e -> {
                e.getMessage().setHeader(ce.mandatoryAttribute("version").id(), ce.version());
                e.getMessage().setHeader(ce.mandatoryAttribute("type").id(), "org.apache.camel.event");
                e.getMessage().setHeader(ce.mandatoryAttribute("id").id(), "myEventID2");
                e.getMessage().setHeader(ce.mandatoryAttribute("time").id(), DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now()));
                e.getMessage().setHeader(ce.mandatoryAttribute("source").id(), "CE2");
            }
        );

        mock1.assertIsSatisfied();
        mock2.assertIsSatisfied();
    }

    @ParameterizedTest
    @MethodSource("provideCloudEventsImplementations")
    void testConsumeContentWithRegExFilter(CloudEvent ce) throws Exception {
        configureKnativeComponent(
            context,
            ce,
            endpoint(
                Knative.EndpointKind.source,
                "ep1",
                "localhost",
                port,
                KnativeSupport.mapOf(
                    Knative.KNATIVE_EVENT_TYPE, "org.apache.camel.event",
                    Knative.CONTENT_TYPE, "text/plain",
                    Knative.KNATIVE_FILTER_PREFIX + ce.mandatoryAttribute("source").id(), "CE[01234]"
                )),
            endpoint(
                Knative.EndpointKind.source,
                "ep2",
                "localhost",
                port,
                KnativeSupport.mapOf(
                    Knative.KNATIVE_EVENT_TYPE, "org.apache.camel.event",
                    Knative.CONTENT_TYPE, "text/plain",
                    Knative.KNATIVE_FILTER_PREFIX + ce.mandatoryAttribute("source").id(), "CE[56789]"
                ))
        );

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
                    .toD("undertow:http://localhost:" + port);
            }
        });

        context.start();

        MockEndpoint mock1 = context.getEndpoint("mock:ce1", MockEndpoint.class);
        mock1.expectedMessagesMatches(e -> e.getMessage().getHeaders().containsKey(ce.mandatoryAttribute("time").id()));
        mock1.expectedHeaderReceived(ce.mandatoryAttribute("version").id(), ce.version());
        mock1.expectedHeaderReceived(ce.mandatoryAttribute("type").id(), "org.apache.camel.event");
        mock1.expectedHeaderReceived(ce.mandatoryAttribute("id").id(), "myEventID1");
        mock1.expectedHeaderReceived(ce.mandatoryAttribute("source").id(), "CE0");
        mock1.expectedBodiesReceived("test");
        mock1.expectedMessageCount(1);

        MockEndpoint mock2 = context.getEndpoint("mock:ce2", MockEndpoint.class);
        mock2.expectedMessagesMatches(e -> e.getMessage().getHeaders().containsKey(ce.mandatoryAttribute("time").id()));
        mock2.expectedHeaderReceived(ce.mandatoryAttribute("version").id(), ce.version());
        mock2.expectedHeaderReceived(ce.mandatoryAttribute("type").id(), "org.apache.camel.event");
        mock2.expectedHeaderReceived(ce.mandatoryAttribute("id").id(), "myEventID2");
        mock2.expectedHeaderReceived(ce.mandatoryAttribute("source").id(), "CE5");
        mock2.expectedBodiesReceived("test");
        mock2.expectedMessageCount(1);

        context.createProducerTemplate().send(
            "direct:source",
            e -> {
                e.getMessage().setHeader(ce.mandatoryAttribute("version").id(), ce.version());
                e.getMessage().setHeader(ce.mandatoryAttribute("type").id(), "org.apache.camel.event");
                e.getMessage().setHeader(ce.mandatoryAttribute("id").id(), "myEventID1");
                e.getMessage().setHeader(ce.mandatoryAttribute("time").id(), DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now()));
                e.getMessage().setHeader(ce.mandatoryAttribute("source").id(), "CE0");
            }
        );
        context.createProducerTemplate().send(
            "direct:source",
            e -> {
                e.getMessage().setHeader(ce.mandatoryAttribute("version").id(), ce.version());
                e.getMessage().setHeader(ce.mandatoryAttribute("type").id(), "org.apache.camel.event");
                e.getMessage().setHeader(ce.mandatoryAttribute("id").id(), "myEventID2");
                e.getMessage().setHeader(ce.mandatoryAttribute("time").id(), DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now()));
                e.getMessage().setHeader(ce.mandatoryAttribute("source").id(), "CE5");
            }
        );

        mock1.assertIsSatisfied();
        mock2.assertIsSatisfied();
    }

    @ParameterizedTest
    @MethodSource("provideCloudEventsImplementations")
    void testConsumeEventContent(CloudEvent ce) throws Exception {
        configureKnativeComponent(
            context,
            ce,
            event(
                Knative.EndpointKind.source,
                "default",
                "localhost",
                port)
        );

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
        mock1.expectedMessagesMatches(e -> e.getMessage().getHeaders().containsKey(ce.mandatoryAttribute("time").id()));
        mock1.expectedHeaderReceived(ce.mandatoryAttribute("version").id(), ce.version());
        mock1.expectedHeaderReceived(ce.mandatoryAttribute("type").id(), "event1");
        mock1.expectedHeaderReceived(ce.mandatoryAttribute("id").id(), "myEventID1");
        mock1.expectedHeaderReceived(ce.mandatoryAttribute("source").id(), "CE1");
        mock1.expectedBodiesReceived("test");
        mock1.expectedMessageCount(1);

        MockEndpoint mock2 = context.getEndpoint("mock:ce2", MockEndpoint.class);
        mock2.expectedMessagesMatches(e -> e.getMessage().getHeaders().containsKey(ce.mandatoryAttribute("time").id()));
        mock2.expectedHeaderReceived(ce.mandatoryAttribute("version").id(), ce.version());
        mock2.expectedHeaderReceived(ce.mandatoryAttribute("type").id(), "event2");
        mock2.expectedHeaderReceived(ce.mandatoryAttribute("id").id(), "myEventID2");
        mock2.expectedHeaderReceived(ce.mandatoryAttribute("source").id(), "CE2");
        mock2.expectedBodiesReceived("test");
        mock2.expectedMessageCount(1);

        context.createProducerTemplate().send(
            "direct:source",
            e -> {
                e.getMessage().setHeader(ce.mandatoryAttribute("version").id(), ce.version());
                e.getMessage().setHeader(ce.mandatoryAttribute("type").id(), "event1");
                e.getMessage().setHeader(ce.mandatoryAttribute("id").id(), "myEventID1");
                e.getMessage().setHeader(ce.mandatoryAttribute("time").id(), DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now()));
                e.getMessage().setHeader(ce.mandatoryAttribute("source").id(), "CE1");
            }
        );
        context.createProducerTemplate().send(
            "direct:source",
            e -> {
                e.getMessage().setHeader(ce.mandatoryAttribute("version").id(), ce.version());
                e.getMessage().setHeader(ce.mandatoryAttribute("type").id(), "event2");
                e.getMessage().setHeader(ce.mandatoryAttribute("id").id(), "myEventID2");
                e.getMessage().setHeader(ce.mandatoryAttribute("time").id(), DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now()));
                e.getMessage().setHeader(ce.mandatoryAttribute("source").id(), "CE2");
            }
        );

        mock1.assertIsSatisfied();
        mock2.assertIsSatisfied();
    }

    @ParameterizedTest
    @MethodSource("provideCloudEventsImplementations")
    void testReply(CloudEvent ce) throws Exception {
        configureKnativeComponent(
            context,
            ce,
            endpoint(
                Knative.EndpointKind.source,
                "from",
                "localhost",
                port,
                KnativeSupport.mapOf(
                    Knative.KNATIVE_EVENT_TYPE, "org.apache.camel.event",
                    Knative.CONTENT_TYPE, "text/plain"
                )),
            endpoint(
                Knative.EndpointKind.sink,
                "to",
                "localhost",
                port,
                KnativeSupport.mapOf(
                    Knative.KNATIVE_EVENT_TYPE, "org.apache.camel.event",
                    Knative.CONTENT_TYPE, "text/plain"
                ))
        );

        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("knative:endpoint/from")
                    .convertBodyTo(String.class)
                    .setBody()
                        .constant("consumer");
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

    @ParameterizedTest
    @MethodSource("provideCloudEventsImplementations")
    void testInvokeServiceWithoutHost(CloudEvent ce) throws Exception {
        configureKnativeComponent(
            context,
            ce,
            endpoint(
                Knative.EndpointKind.sink,
                "test",
                "",
                port,
                KnativeSupport.mapOf(
                    Knative.KNATIVE_EVENT_TYPE, "org.apache.camel.event",
                    Knative.CONTENT_TYPE, "text/plain"
                )
            )
        );

        RouteBuilder.addRoutes(context, b -> {
            b.from("direct:start")
                .to("knative:endpoint/test")
                .to("mock:start");
        });

        context.start();

        Exchange exchange = template.request("direct:start", e -> e.getMessage().setBody(""));
        assertThat(exchange.isFailed()).isTrue();
        assertThat(exchange.getException()).isInstanceOf(CamelException.class);
        assertThat(exchange.getException()).hasMessageStartingWith("HTTP operation failed because host is not defined");
    }

    @ParameterizedTest
    @MethodSource("provideCloudEventsImplementations")
    void testInvokeNotExistingEndpoint(CloudEvent ce) throws Exception {
        configureKnativeComponent(
            context,
            ce,
            endpoint(
                Knative.EndpointKind.sink,
                "test",
                "localhost",
                port,
                KnativeSupport.mapOf(
                    Knative.KNATIVE_EVENT_TYPE, "org.apache.camel.event",
                    Knative.CONTENT_TYPE, "text/plain"
                )
            )
        );

        RouteBuilder.addRoutes(context, b -> {
            b.from("direct:start")
                .to("knative:endpoint/test")
                .to("mock:start");
        });

        context.start();

        Exchange exchange = template.request("direct:start", e -> e.getMessage().setBody(""));
        assertThat(exchange.isFailed()).isTrue();
        assertThat(exchange.getException()).isInstanceOf(CamelException.class);
        assertThat(exchange.getException()).hasMessageStartingWith("HTTP operation failed invoking http://localhost:" + port + "/");
    }

    @ParameterizedTest
    @MethodSource("provideCloudEventsImplementations")
    void testRemoveConsumer(CloudEvent ce) throws Exception {
        configureKnativeComponent(
            context,
            ce,
            endpoint(
                Knative.EndpointKind.source,
                "ep1",
                "localhost",
                port,
                KnativeSupport.mapOf(
                    Knative.KNATIVE_EVENT_TYPE, "org.apache.camel.event",
                    Knative.CONTENT_TYPE, "text/plain",
                    Knative.KNATIVE_FILTER_PREFIX + "h", "h1"
                )
            ),
            endpoint(
                Knative.EndpointKind.source,
                "ep2",
                "localhost",
                port,
                KnativeSupport.mapOf(
                    Knative.KNATIVE_EVENT_TYPE, "org.apache.camel.event",
                    Knative.CONTENT_TYPE, "text/plain",
                    Knative.KNATIVE_FILTER_PREFIX + "h", "h2"
                )
            )
        );

        RouteBuilder.addRoutes(context, b -> {
            b.from("knative:endpoint/ep1")
                .routeId("r1")
                .setBody().simple("${routeId}");
            b.from("knative:endpoint/ep2")
                .routeId("r2")
                .setBody().simple("${routeId}");
        });
        RouteBuilder.addRoutes(context, b -> {
            b.from("direct:start")
                .setHeader("h").body()
                .toF("undertow:http://localhost:%d", port);
        });

        context.start();

        assertThat(template.requestBody("direct:start", "h1", String.class)).isEqualTo("r1");
        assertThat(template.requestBody("direct:start", "h2", String.class)).isEqualTo("r2");

        context.getRouteController().stopRoute("r2");

        assertThat(template.request("direct:start", e -> e.getMessage().setBody("h2"))).satisfies(e -> {
            assertThat(e.isFailed()).isTrue();
            assertThat(e.getException()).isInstanceOf(HttpOperationFailedException.class);
        });
    }

    @ParameterizedTest
    @MethodSource("provideCloudEventsImplementations")
    void testAddConsumer(CloudEvent ce) throws Exception {
        configureKnativeComponent(
            context,
            ce,
            endpoint(
                Knative.EndpointKind.source,
                "ep1",
                "localhost",
                port,
                KnativeSupport.mapOf(
                    Knative.KNATIVE_EVENT_TYPE, "org.apache.camel.event",
                    Knative.CONTENT_TYPE, "text/plain",
                    Knative.KNATIVE_FILTER_PREFIX + "h", "h1"
                )
            ),
            endpoint(
                Knative.EndpointKind.source,
                "ep2",
                "localhost",
                port,
                KnativeSupport.mapOf(
                    Knative.KNATIVE_EVENT_TYPE, "org.apache.camel.event",
                    Knative.CONTENT_TYPE, "text/plain",
                    Knative.KNATIVE_FILTER_PREFIX + "h", "h2"
                )
            )
        );

        RouteBuilder.addRoutes(context, b -> {
            b.from("knative:endpoint/ep1")
                .routeId("r1")
                .setBody().simple("${routeId}");
        });
        RouteBuilder.addRoutes(context, b -> {
            b.from("direct:start")
                .setHeader("h").body()
                .toF("undertow:http://localhost:%d", port);
        });

        context.start();

        assertThat(template.requestBody("direct:start", "h1", String.class)).isEqualTo("r1");
        assertThat(template.request("direct:start", e -> e.getMessage().setBody("h2"))).satisfies(e -> {
            assertThat(e.isFailed()).isTrue();
            assertThat(e.getException()).isInstanceOf(HttpOperationFailedException.class);
        });

        RouteBuilder.addRoutes(context, b -> {
            b.from("knative:endpoint/ep2")
                .routeId("r2")
                .setBody().simple("${routeId}");
        });

        assertThat(template.requestBody("direct:start", "h1", String.class)).isEqualTo("r1");
        assertThat(template.requestBody("direct:start", "h2", String.class)).isEqualTo("r2");
    }

    @ParameterizedTest
    @MethodSource("provideCloudEventsImplementations")
    void testInvokeEndpointWithError(CloudEvent ce) throws Exception {
        configureKnativeComponent(
            context,
            ce,
            endpoint(
                Knative.EndpointKind.sink,
                "ep",
                "localhost",
                port,
                KnativeSupport.mapOf(
                    Knative.KNATIVE_EVENT_TYPE, "org.apache.camel.event",
                    Knative.CONTENT_TYPE, "text/plain"
                )
            )
        );

        RouteBuilder.addRoutes(context, b -> {
            b.from("direct:start")
                .to("knative:endpoint/ep")
                .to("mock:start");
            b.fromF("undertow:http://0.0.0.0:%d", port)
                .routeId("endpoint")
                .process(e -> {
                    throw new RuntimeException("endpoint error");
                });
        });

        context.start();

        Exchange exchange = template.request("direct:start", e -> e.getMessage().setBody(""));
        assertThat(exchange.isFailed()).isTrue();
        assertThat(exchange.getException()).isInstanceOf(CamelException.class);
        assertThat(exchange.getException()).hasMessageStartingWith("HTTP operation failed invoking");
        assertThat(exchange.getException()).hasMessageContaining("with statusCode: 500, statusMessage: Internal Server Error");
    }

    @ParameterizedTest
    @MethodSource("provideCloudEventsImplementations")
    void testEvents(CloudEvent ce) throws Exception {
        configureKnativeComponent(
            context,
            ce,
            event(
                Knative.EndpointKind.sink,
                "default",
                "localhost",
                port,
                KnativeSupport.mapOf(
                    Knative.KNATIVE_EVENT_TYPE, "org.apache.camel.event",
                    Knative.CONTENT_TYPE, "text/plain"
                )),
            event(
                Knative.EndpointKind.source,
                "default",
                "localhost",
                port,
                KnativeSupport.mapOf(
                    Knative.KNATIVE_EVENT_TYPE, "org.apache.camel.event",
                    Knative.CONTENT_TYPE, "text/plain"
                ))
        );

        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:source")
                    .to("knative:event/myEvent");
                fromF("knative:event/myEvent")
                    .to("mock:ce");
            }
        });

        context.start();

        MockEndpoint mock = context.getEndpoint("mock:ce", MockEndpoint.class);
        mock.expectedHeaderReceived(ce.mandatoryAttribute("version").id(), ce.version());
        mock.expectedHeaderReceived(ce.mandatoryAttribute("type").id(), "myEvent");
        mock.expectedHeaderReceived(Exchange.CONTENT_TYPE, "text/plain");
        mock.expectedMessagesMatches(e -> e.getMessage().getHeaders().containsKey(ce.mandatoryAttribute("time").id()));
        mock.expectedMessagesMatches(e -> e.getMessage().getHeaders().containsKey(ce.mandatoryAttribute("id").id()));
        mock.expectedBodiesReceived("test");
        mock.expectedMessageCount(1);

        context.createProducerTemplate().sendBody("direct:source", "test");

        mock.assertIsSatisfied();
    }

    @ParameterizedTest
    @MethodSource("provideCloudEventsImplementations")
    void testEventsWithTypeAndVersion(CloudEvent ce) throws Exception {
        configureKnativeComponent(
            context,
            ce,
            event(
                Knative.EndpointKind.sink,
                "default",
                "localhost",
                port,
                KnativeSupport.mapOf(
                    Knative.KNATIVE_EVENT_TYPE, "org.apache.camel.event",
                    Knative.CONTENT_TYPE, "text/plain",
                    Knative.KNATIVE_KIND, "MyObject",
                    Knative.KNATIVE_API_VERSION, "v1"
                )),
            event(
                Knative.EndpointKind.source,
                "default",
                "localhost",
                port,
                KnativeSupport.mapOf(
                    Knative.KNATIVE_EVENT_TYPE, "org.apache.camel.event",
                    Knative.CONTENT_TYPE, "text/plain",
                    Knative.KNATIVE_KIND, "MyOtherObject",
                    Knative.KNATIVE_API_VERSION, "v2"
                ))
        );

        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:source")
                    .to("knative:event/myEvent?kind=MyObject&apiVersion=v1");

                fromF("knative:event/myEvent?kind=MyOtherObject&apiVersion=v2")
                    .to("mock:ce");
            }
        });

        context.start();

        MockEndpoint mock = context.getEndpoint("mock:ce", MockEndpoint.class);
        mock.expectedHeaderReceived(ce.mandatoryAttribute("version").id(), ce.version());
        mock.expectedHeaderReceived(ce.mandatoryAttribute("type").id(), "myEvent");
        mock.expectedHeaderReceived(Exchange.CONTENT_TYPE, "text/plain");
        mock.expectedMessagesMatches(e -> e.getMessage().getHeaders().containsKey(ce.mandatoryAttribute("time").id()));
        mock.expectedMessagesMatches(e -> e.getMessage().getHeaders().containsKey(ce.mandatoryAttribute("id").id()));
        mock.expectedBodiesReceived("test");
        mock.expectedMessageCount(1);

        context.createProducerTemplate().sendBody("direct:source", "test");

        mock.assertIsSatisfied();
    }

    @ParameterizedTest
    @MethodSource("provideCloudEventsImplementations")
    void testConsumeContentWithTypeAndVersion(CloudEvent ce) throws Exception {
        configureKnativeComponent(
            context,
            ce,
            endpoint(
                Knative.EndpointKind.source,
                "myEndpoint",
                "localhost",
                port + 1,
                KnativeSupport.mapOf(
                    Knative.KNATIVE_EVENT_TYPE, "org.apache.camel.event",
                    Knative.CONTENT_TYPE, "text/plain",
                    Knative.KNATIVE_KIND, "MyObject",
                    Knative.KNATIVE_API_VERSION, "v1"
                )),
            endpoint(
                Knative.EndpointKind.source,
                "myEndpoint",
                "localhost",
                port,
                KnativeSupport.mapOf(
                    Knative.KNATIVE_EVENT_TYPE, "org.apache.camel.event",
                    Knative.CONTENT_TYPE, "text/plain",
                    Knative.KNATIVE_KIND, "MyObject",
                    Knative.KNATIVE_API_VERSION, "v2"
                ))
        );

        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("knative:endpoint/myEndpoint?kind=MyObject&apiVersion=v2")
                    .to("mock:ce");
                from("direct:source")
                    .toF("undertow:http://localhost:%d", port);
            }
        });

        context.start();

        MockEndpoint mock = context.getEndpoint("mock:ce", MockEndpoint.class);
        mock.expectedHeaderReceived(ce.mandatoryAttribute("version").id(), ce.version());
        mock.expectedHeaderReceived(ce.mandatoryAttribute("type").id(), "org.apache.camel.event");
        mock.expectedHeaderReceived(ce.mandatoryAttribute("id").id(), "myEventID");
        mock.expectedHeaderReceived(ce.mandatoryAttribute("source").id(), "/somewhere");
        mock.expectedHeaderReceived(Exchange.CONTENT_TYPE, "text/plain");
        mock.expectedMessagesMatches(e -> e.getMessage().getHeaders().containsKey(ce.mandatoryAttribute("time").id()));
        mock.expectedBodiesReceived("test");
        mock.expectedMessageCount(1);

        context.createProducerTemplate().send(
            "direct:source",
            e -> {
                e.getMessage().setHeader(Exchange.CONTENT_TYPE, "text/plain");
                e.getMessage().setHeader(ce.mandatoryAttribute("version").id(), ce.version());
                e.getMessage().setHeader(ce.mandatoryAttribute("type").id(), "org.apache.camel.event");
                e.getMessage().setHeader(ce.mandatoryAttribute("id").id(), "myEventID");
                e.getMessage().setHeader(ce.mandatoryAttribute("time").id(), DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now()));
                e.getMessage().setHeader(ce.mandatoryAttribute("source").id(), "/somewhere");
                e.getMessage().setBody("test");
            }
        );

        mock.assertIsSatisfied();
    }

    @ParameterizedTest
    @MethodSource("provideCloudEventsImplementations")
    void testWrongMethod(CloudEvent ce) throws Exception {
        configureKnativeComponent(
            context,
            ce,
            endpoint(
                Knative.EndpointKind.source,
                "myEndpoint",
                "localhost",
                port,
                KnativeSupport.mapOf(
                    Knative.KNATIVE_EVENT_TYPE, "org.apache.camel.event",
                    Knative.CONTENT_TYPE, "text/plain"
                ))
        );

        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("knative:endpoint/myEndpoint")
                    .to("mock:ce");
                from("direct:start")
                    .toF("undertow:http://localhost:%d", port);
            }
        });

        context.start();

        Exchange exchange = template.request("direct:start", e -> e.getMessage().setBody(null));
        assertThat(exchange.isFailed()).isTrue();
        assertThat(exchange.getException()).isInstanceOf(CamelException.class);
        assertThat(exchange.getException()).hasMessageStartingWith("HTTP operation failed invoking");
        assertThat(exchange.getException()).hasMessageEndingWith("with statusCode: 405");
    }

    @ParameterizedTest
    @MethodSource("provideCloudEventsImplementations")
    void testNoBody(CloudEvent ce) throws Exception {
        configureKnativeComponent(
            context,
            ce,
            endpoint(
                Knative.EndpointKind.sink,
                "myEndpoint",
                "localhost",
                port,
                KnativeSupport.mapOf(
                    Knative.KNATIVE_EVENT_TYPE, "org.apache.camel.event",
                    Knative.CONTENT_TYPE, "text/plain"
                ))
        );

        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                    .to("knative:endpoint/myEndpoint");
            }
        });

        context.start();

        Exchange exchange = template.request("direct:start", e -> e.getMessage().setBody(null));
        assertThat(exchange.isFailed()).isTrue();
        assertThat(exchange.getException()).isInstanceOf(IllegalArgumentException.class);
        assertThat(exchange.getException()).hasMessage("body must not be null");
    }

    @ParameterizedTest
    @MethodSource("provideCloudEventsImplementations")
    void testNoContent(CloudEvent ce) throws Exception {
        final int messagesPort = AvailablePortFinder.getNextAvailable();
        final int wordsPort = AvailablePortFinder.getNextAvailable();

        configureKnativeComponent(
            context,
            ce,
            channel(
                Knative.EndpointKind.source,
                "messages",
                "localhost",
                messagesPort,
                KnativeSupport.mapOf(
                    Knative.KNATIVE_EVENT_TYPE, "org.apache.camel.event",
                    Knative.CONTENT_TYPE, "text/plain"
                )),
            channel(
                Knative.EndpointKind.sink,
                "messages",
                "localhost",
                messagesPort,
                KnativeSupport.mapOf(
                    Knative.KNATIVE_EVENT_TYPE, "org.apache.camel.event",
                    Knative.CONTENT_TYPE, "text/plain"
                )),
            channel(
                Knative.EndpointKind.sink,
                "words",
                "localhost",
                wordsPort,
                KnativeSupport.mapOf(
                    Knative.KNATIVE_EVENT_TYPE, "org.apache.camel.event",
                    Knative.CONTENT_TYPE, "text/plain"
                ))
        );

        Undertow server = Undertow.builder()
            .addHttpListener(wordsPort, "localhost")
            .setHandler(exchange -> {
                exchange.setStatusCode(204);
                exchange.getResponseSender().send("");
            })
            .build();

        try {
            server.start();

            context.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("knative:channel/messages")
                        .transform().simple("transformed ${body}")
                        .log("${body}")
                        .to("knative:channel/words");
                }
            });

            context.start();

            Exchange exchange = template.request("knative:channel/messages", e -> e.getMessage().setBody("message"));
            assertThat(exchange.getMessage().getHeaders()).containsEntry(Exchange.HTTP_RESPONSE_CODE, 204);
            assertThat(exchange.getMessage().getBody()).isNull();
        } finally {
            server.stop();
        }
    }

    @ParameterizedTest
    @MethodSource("provideCloudEventsImplementations")
    void testOrdering(CloudEvent ce) throws Exception {
        List<KnativeEnvironment.KnativeServiceDefinition> hops = new Random()
            .ints(0, 100)
            .distinct()
            .limit(10)
            .mapToObj(i -> endpoint(
                Knative.EndpointKind.source,
                "channel-" + i,
                "localhost",
                port,
                KnativeSupport.mapOf(Knative.KNATIVE_FILTER_PREFIX + "MyHeader", "channel-" + i)))
            .collect(Collectors.toList());

        configureKnativeComponent(context, ce, hops);

        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                    .routeId("undertow")
                    .toF("undertow:http://localhost:%d", port)
                    .convertBodyTo(String.class);

                for (KnativeEnvironment.KnativeServiceDefinition definition: hops) {
                    fromF("knative:endpoint/%s", definition.getName())
                        .routeId(definition.getName())
                        .setBody().constant(definition.getName());
                }
            }
        });

        context.start();

        List<String> hopsDone = new ArrayList<>();
        for (KnativeEnvironment.KnativeServiceDefinition definition: hops) {
            hopsDone.add(definition.getName());

            Exchange result = template.request(
                "direct:start",
                e -> {
                    e.getMessage().setHeader("MyHeader", hopsDone);
                    e.getMessage().setBody(definition.getName());
                }
            );

            assertThat(result.getMessage().getBody()).isEqualTo(definition.getName());
        }
    }
}


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

import org.apache.camel.CamelContext;
import org.apache.camel.CamelException;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.http.common.HttpOperationFailedException;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.test.AvailablePortFinder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    void testWithPaths() throws Exception {
        RouteBuilder.addRoutes(context, b -> {
            b.fromF("knative-http:0.0.0.0:%d/a/1", port)
                .routeId("r1")
                .setBody().simple("${routeId}")
                .to("mock:r1");
            b.fromF("knative-http:0.0.0.0:%d/a/2", port)
                .routeId("r2")
                .setBody().simple("${routeId}")
                .to("mock:r2");

            b.from("direct:start")
                .toD("undertow:http://localhost:" + port + "/a/${body}");
        });

        context.getEndpoint("mock:r1", MockEndpoint.class).expectedMessageCount(1);
        context.getEndpoint("mock:r2", MockEndpoint.class).expectedMessageCount(1);
        context.start();

        assertThat(template.requestBody("direct:start", "1", String.class)).isEqualTo("r1");
        assertThat(template.requestBody("direct:start", "2", String.class)).isEqualTo("r2");

        MockEndpoint.assertIsSatisfied(context);
    }

    @Test
    void testWithFilters() throws Exception {
        RouteBuilder.addRoutes(context, b -> {
            b.fromF("knative-http:0.0.0.0:%d?filter.MyHeader=h1", port)
                .routeId("r1")
                .setBody().simple("${routeId}")
                .to("mock:r1");
            b.fromF("knative-http:0.0.0.0:%d?filter.myheader=h2", port)
                .routeId("r2")
                .setBody().simple("${routeId}")
                .to("mock:r2");
            b.fromF("knative-http:0.0.0.0:%d?filter.myheader=t.*", port)
                .routeId("r3")
                .setBody().simple("${routeId}")
                .to("mock:r3");

            b.from("direct:start")
                .setHeader("MyHeader").body()
                .toF("undertow:http://localhost:%d", port);
        });

        context.getEndpoint("mock:r1", MockEndpoint.class).expectedMessageCount(1);
        context.getEndpoint("mock:r2", MockEndpoint.class).expectedMessageCount(1);
        context.start();

        assertThat(template.requestBody("direct:start", "h1", String.class)).isEqualTo("r1");
        assertThat(template.requestBody("direct:start", "h2", String.class)).isEqualTo("r2");
        assertThat(template.requestBody("direct:start", "t1", String.class)).isEqualTo("r3");
        assertThat(template.requestBody("direct:start", "t2", String.class)).isEqualTo("r3");

        MockEndpoint.assertIsSatisfied(context);
    }

    @Test
    void testWithRexFilters() throws Exception {
        RouteBuilder.addRoutes(context, b -> {
            b.fromF("knative-http:0.0.0.0:%d?filter.MyHeader=h.*", port)
                .routeId("r1")
                .setBody().simple("${routeId}");

            b.from("direct:start")
                .setHeader("MyHeader").body()
                .toF("undertow:http://localhost:%d", port);
        });

        context.start();

        assertThat(template.requestBody("direct:start", "h1", String.class)).isEqualTo("r1");
        assertThat(template.request("direct:start", e -> e.getMessage().setBody("t1"))).satisfies(e -> {
            assertThat(e.isFailed()).isTrue();
            assertThat(e.getException()).isInstanceOf(HttpOperationFailedException.class);
        });
    }

    @Test
    void testRemoveConsumer() throws Exception {
        RouteBuilder.addRoutes(context, b -> {
            b.fromF("knative-http:0.0.0.0:%d?filter.h=h1", port)
                .routeId("r1")
                .setBody().simple("${routeId}");
            b.fromF("knative-http:0.0.0.0:%d?filter.h=h2", port)
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

    @Test
    void testAddConsumer() throws Exception {
        RouteBuilder.addRoutes(context, b -> {
            b.fromF("knative-http:0.0.0.0:%d?filter.h=h1", port)
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
            b.fromF("knative-http:0.0.0.0:%d?filter.h=h2", port)
                .routeId("r2")
                .setBody().simple("${routeId}");
        });

        assertThat(template.requestBody("direct:start", "h1", String.class)).isEqualTo("r1");
        assertThat(template.requestBody("direct:start", "h2", String.class)).isEqualTo("r2");
    }

    @Test
    void testInvokeEndpoint() throws Exception {
        RouteBuilder.addRoutes(context, b -> {
            b.fromF("undertow:http://0.0.0.0:%d", port)
                .routeId("endpoint")
                .setBody().simple("${routeId}")
                .to("mock:endpoint");

            b.from("direct:start")
                .toF("knative-http:0.0.0.0:%d", port)
                .to("mock:start");
        });

        MockEndpoint mock1 = context.getEndpoint("mock:endpoint", MockEndpoint.class);
        mock1.expectedHeaderReceived("Host", "0.0.0.0");
        mock1.expectedMessageCount(1);

        MockEndpoint mock2 = context.getEndpoint("mock:start", MockEndpoint.class);
        mock2.expectedBodiesReceived("endpoint");
        mock2.expectedMessageCount(1);

        context.start();

        template.sendBody("direct:start", "1");

        mock1.assertIsSatisfied();
        mock2.assertIsSatisfied();
    }

    @Test
    void testInvokeNotExistingEndpoint() throws Exception {
        RouteBuilder.addRoutes(context, b -> {
            b.from("direct:start")
                .toF("knative-http:0.0.0.0:%d", port)
                .to("mock:start");
        });

        context.start();

        Exchange exchange = template.request("direct:start", e -> e.getMessage().setBody(""));
        assertThat(exchange.isFailed()).isTrue();
        assertThat(exchange.getException()).isInstanceOf(CamelException.class);
        assertThat(exchange.getException()).hasMessageStartingWith("HTTP operation failed invoking");
    }

    @Test
    void testInvokeEndpointWithError() throws Exception {
        RouteBuilder.addRoutes(context, b -> {
            b.from("direct:start")
                .toF("knative-http:0.0.0.0:%d", port)
                .to("mock:start");
            b.fromF("undertow:http://0.0.0.0:%d", port)
                .routeId("endpoint")
                .process(e -> { throw new RuntimeException("endpoint error"); });
        });

        context.start();

        Exchange exchange = template.request("direct:start", e -> e.getMessage().setBody(""));
        assertThat(exchange.isFailed()).isTrue();
        assertThat(exchange.getException()).isInstanceOf(CamelException.class);
        assertThat(exchange.getException()).hasMessageStartingWith("HTTP operation failed invoking");
        assertThat(exchange.getException()).hasMessageContaining("with statusCode: 500, statusMessage: Internal Server Error");
    }
}


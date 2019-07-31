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
import org.apache.camel.CamelExecutionException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    public void after() throws Exception {
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
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                fromF("knative-http:0.0.0.0:%d/a/1", port)
                    .routeId("r1")
                    .setBody().simple("${routeId}")
                    .convertBodyTo(String.class)
                    .to("mock:r1");
                fromF("knative-http:0.0.0.0:%d/a/2", port)
                    .routeId("r2")
                    .setBody().simple("${routeId}")
                    .convertBodyTo(String.class)
                    .to("mock:r2");
                from("direct:start")
                    .toD("undertow:http://localhost:" + port + "/a/${body}");
                }
            }
        );

        MockEndpoint m1 = context.getEndpoint("mock:r1", MockEndpoint.class);
        m1.expectedMessageCount(1);
        m1.expectedBodiesReceived("r1");

        MockEndpoint m2 = context.getEndpoint("mock:r2", MockEndpoint.class);
        m2.expectedMessageCount(1);
        m2.expectedBodiesReceived("r2");

        context.start();

        assertThat(
            template.requestBody("direct:start", "1", String.class)
        ).isEqualTo("r1");
        assertThat(
            template.requestBody("direct:start", "2", String.class)
        ).isEqualTo("r2");

        m1.assertIsSatisfied();
        m2.assertIsSatisfied();
    }

    @Test
    void testWithFilters() throws Exception {
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                fromF("knative-http:0.0.0.0:%d?filter.MyHeader=h1", port)
                    .routeId("r1")
                    .setBody().simple("${routeId}")
                    .convertBodyTo(String.class)
                    .to("mock:r1");
                fromF("knative-http:0.0.0.0:%d?filter.myheader=h2", port)
                    .routeId("r2")
                    .setBody().simple("${routeId}")
                    .convertBodyTo(String.class)
                    .to("mock:r2");
                fromF("knative-http:0.0.0.0:%d?filter.myheader=t.*", port)
                    .routeId("r3")
                    .setBody().simple("${routeId}")
                    .convertBodyTo(String.class)
                    .to("mock:r3");
                from("direct:start")
                    .setHeader("MyHeader").body()
                    .toF("undertow:http://localhost:%d", port);
                }
            }
        );

        MockEndpoint m1 = context.getEndpoint("mock:r1", MockEndpoint.class);
        m1.expectedMessageCount(1);
        m1.expectedBodiesReceived("r1");

        MockEndpoint m2 = context.getEndpoint("mock:r2", MockEndpoint.class);
        m2.expectedMessageCount(1);
        m2.expectedBodiesReceived("r2");

        context.start();

        assertThat(
            template.requestBody("direct:start", "h1", String.class)
        ).isEqualTo("r1");
        assertThat(
            template.requestBody("direct:start", "h2", String.class)
        ).isEqualTo("r2");
        assertThat(
            template.requestBody("direct:start", "t1", String.class)
        ).isEqualTo("r3");
        assertThat(
            template.requestBody("direct:start", "t2", String.class)
        ).isEqualTo("r3");

        m1.assertIsSatisfied();
        m2.assertIsSatisfied();
    }

    @Test
    void testWithRexFilters() throws Exception {
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                fromF("knative-http:0.0.0.0:%d?filter.MyHeader=h.*", port)
                    .routeId("r1")
                    .setBody().simple("${routeId}")
                    .convertBodyTo(String.class);
                from("direct:start")
                    .setHeader("MyHeader").body()
                    .toF("undertow:http://localhost:%d", port);
                }
            }
        );

        context.start();

        assertThat(
            template.requestBody("direct:start", "h1", String.class)
        ).isEqualTo("r1");
        assertThatThrownBy(
            () -> template.requestBody("direct:start", "t1", String.class)
        ).isInstanceOf(CamelExecutionException.class).hasCauseExactlyInstanceOf(HttpOperationFailedException.class);
    }

    @Test
    void testInvokeEndpoint() throws Exception {
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                fromF("undertow:http://0.0.0.0:%d", port)
                    .routeId("endpoint")
                    .setBody().simple("${routeId}")
                    .convertBodyTo(String.class)
                    .to("mock:endpoint");
                from("direct:start")
                    .toF("knative-http:0.0.0.0:%d", port);
                }
            }
        );

        MockEndpoint mock = context.getEndpoint("mock:endpoint", MockEndpoint.class);
        mock.expectedMessageCount(1);
        mock.expectedBodiesReceived("endpoint");
        mock.expectedHeaderReceived("Host", "0.0.0.0");

        context.start();

        template.requestBody("direct:start", "1", String.class);

        mock.assertIsSatisfied();
    }
}


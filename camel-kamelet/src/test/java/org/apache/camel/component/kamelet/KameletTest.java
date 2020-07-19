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
package org.apache.camel.component.kamelet;

import java.util.UUID;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class KameletTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(KameletTest.class);

    @Test
    public void test() throws Exception {
        String body = UUID.randomUUID().toString();

        CamelContext context = new DefaultCamelContext();
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                routeTemplate("setBody")
                    .templateParameter("bodyValue")
                    .from("direct:{{routeId}}")
                    .setBody().constant("{{bodyValue}}");
            }
        });

        /*
        context.addRouteFromTemplate("setBody")
            .routeId("test")
            .parameter("routeId", "test")
            .parameter("bodyValue", body)
            .build();
         */

        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                // routes
                from("direct:template")
                    .to("kamelet:setBody/test?bodyValue=bv")
                    .to("log:1");
            }
        });

        context.start();

        assertThat(
            context.createFluentProducerTemplate().to("direct:template").withBody("test").request(String.class)
        ).isEqualTo(body);

        context.stop();
    }
}

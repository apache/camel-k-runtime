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
package org.apache.camel.k.health;

import java.net.URL;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.k.Runtime;
import org.apache.camel.k.http.PlatformHttpServiceContextCustomizer;
import org.apache.camel.k.test.AvailablePortFinder;
import org.apache.camel.util.ObjectHelper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;

public class HealthCustomizerTest {

    @ParameterizedTest
    @ValueSource(strings = { "", "/test", "/test/nested" })
    public void testHealthConfigurer(String path) throws Exception {
        Runtime runtime = Runtime.on(new DefaultCamelContext());
        runtime.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                    .routeId("my-route")
                    .to("mock:end");
            }
        });

        PlatformHttpServiceContextCustomizer phsc = new PlatformHttpServiceContextCustomizer();
        phsc.setBindPort(AvailablePortFinder.getNextAvailable());

        String url;
        if (ObjectHelper.isEmpty(path)) {
            url = "http://localhost:" + phsc.getBindPort() + HealthContextCustomizer.DEFAULT_PATH;
        } else {
            phsc.setPath(path);

            url = "http://localhost:" + phsc.getBindPort() + path + HealthContextCustomizer.DEFAULT_PATH;
        }

        phsc.apply(runtime.getCamelContext());

        HealthContextCustomizer healthCustomizer = new HealthContextCustomizer();
        healthCustomizer.apply(runtime.getCamelContext());

        try {
            runtime.getCamelContext().start();

            when()
                .get(new URL(url))
            .then()
                .statusCode(200)
                .body(
                    "status", equalTo("UP"),
                    "checks.name", hasItems("context", "route:my-route"),
                    "checks.status", hasItems("UP", "UP")
                );
        } finally {
            runtime.stop();
        }
    }
}

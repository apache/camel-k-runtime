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
package org.apache.camel.k.quarkus.it.knative.source;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.MediaType;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@QuarkusTestResource(KnativeSourceYamlTest.Resource.class)
@QuarkusTest
public class KnativeSourceYamlTest {
    @Test
    @Order(0)
    public void validateEndpointUris() {
        List<String> p = given()
            .accept(MediaType.APPLICATION_JSON)
            .get("/test/inspect/endpoint-uris")
            .then()
            .statusCode(200)
            .extract()
            .body()
            .jsonPath().getList(".", String.class);

        assertThat(p).contains("knative://endpoint/sink");
    }

    @Test
    @Order(1)
    public void validateBehavior() {
        final String body = "test";

        given()
            .body(body)
            .post("/test/send")
            .then()
            .statusCode(204);

        await().atMost(10, TimeUnit.SECONDS).until(() -> {
            var response = given()
                .accept(MediaType.TEXT_PLAIN)
                .get("/test/poll");

            return response.statusCode() == 200
                && body.toUpperCase(Locale.US).equals(response.body().asString());
        });

    }

    public static class Resource implements QuarkusTestResourceLifecycleManager {
        @Override
        public Map<String, String> start() {
            return Map.of(
                "kamel.k.sink.port",
                System.getProperty("camel.k.test.knative.listenting.port", "8081"));
        }

        @Override
        public void stop() {
        }
    }
}
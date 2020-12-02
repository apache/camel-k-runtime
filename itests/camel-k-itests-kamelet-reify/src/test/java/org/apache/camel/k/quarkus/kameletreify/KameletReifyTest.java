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
package org.apache.camel.k.quarkus.kameletreify;

import java.util.UUID;

import javax.ws.rs.core.MediaType;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@QuarkusTestResource(KameletReifyTestResource.class)
public class KameletReifyTest {

    @Test
    public void inspect() {
        JsonPath p = RestAssured.given()
            .contentType(MediaType.TEXT_PLAIN)
            .accept(MediaType.APPLICATION_JSON)
            .get("/test/inspect")
            .then()
                .statusCode(200)
                .extract()
                    .body()
                    .jsonPath();

        assertThat(p.getList("components", String.class))
            .filteredOn(n -> n.startsWith("activemq-"))
            .hasSize(2);
        assertThat(p.getList("endpoints", String.class))
            .filteredOn(e ->  e.startsWith("activemq-"))
            .hasSize(2);
        assertThat(p.getList("endpoints", String.class))
            .filteredOn(e ->  e.startsWith("activemq:"))
            .isEmpty();
    }

    @Test
    public void tests() {
        final String body = UUID.randomUUID().toString();

        given()
            .body(body)
            .post("/test/request")
        .then()
            .statusCode(204);

        when()
            .get("/test/receive")
            .then()
                .statusCode(200)
                .body(is(body));

    }
}

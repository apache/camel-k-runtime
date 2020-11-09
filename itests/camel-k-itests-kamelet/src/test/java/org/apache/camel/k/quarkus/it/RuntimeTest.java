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
package org.apache.camel.k.quarkus.it;

import java.util.Locale;

import javax.ws.rs.core.MediaType;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import org.apache.camel.Exchange;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

@QuarkusTest
public class RuntimeTest {

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

        assertThat(p.getList("templates", String.class)).containsExactlyInAnyOrder("to-upper", "set-body");
    }

    @Test
    public void execute() {
        RestAssured.given()
            .accept(MediaType.TEXT_PLAIN)
            .get("/test/execute")
            .then()
                .statusCode(200)
                .body(is("template"));
    }

    @Test
    public void executeTemplate() {
        final String payload = "test";

        String result = given()
            .body(payload)
            .header(Exchange.CONTENT_TYPE, "text/plain")
            .when()
                .post("/test/execute/{templateId}", "to-upper")
            .then()
                .statusCode(200)
            .extract()
                .asString();

        assertThat(result).isEqualTo(payload.toUpperCase(Locale.US));
    }
}
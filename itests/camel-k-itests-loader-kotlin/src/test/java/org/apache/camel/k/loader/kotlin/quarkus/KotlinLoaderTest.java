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
package org.apache.camel.k.loader.kotlin.quarkus;

import java.io.IOException;
import java.io.InputStream;

import jakarta.ws.rs.core.MediaType;

import io.quarkus.test.junit.DisabledOnIntegrationTest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import org.apache.camel.util.IOHelper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled("https://github.com/quarkusio/quarkus/issues/11549")
@DisabledOnIntegrationTest
@QuarkusTest
public class KotlinLoaderTest {
    @Test
    public void testLoadRoutes() throws IOException {
        String code;

        try (InputStream is = KotlinLoaderTest.class.getResourceAsStream("/routes.kts")) {
            code = IOHelper.loadText(is);
        }

        JsonPath p = RestAssured.given()
            .contentType(MediaType.TEXT_PLAIN)
            .accept(MediaType.APPLICATION_JSON)
            .body(code)
            .post("/test/load-routes/MyRoute")
            .then()
                .statusCode(200)
            .extract()
                .body()
                .jsonPath();

        assertThat(p.getList("components", String.class)).contains("direct", "log");
        assertThat(p.getList("routes", String.class)).contains("kotlin");
        assertThat(p.getList("endpoints", String.class)).contains("direct://kotlin", "log://kotlin");
    }
}

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
package org.apache.camel.k.quarkus.it.knative.source.support;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import io.quarkus.test.junit.QuarkusTestProfile;

import static io.restassured.RestAssured.given;
import static org.apache.camel.util.CollectionHelper.mapOf;
import static org.assertj.core.api.Assertions.assertThat;

public final class KnativeSourceTestSupport {
    private KnativeSourceTestSupport() {
    }

    public static void validateEndpointUris() {
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

    public static void validateBehavior() {
        final String body = "test";

        String result = given()
            .accept(MediaType.TEXT_PLAIN)
            .body(body)
            .post("/test/send")
            .then()
                .statusCode(200)
            .extract()
                .body()
                .asString();

        assertThat(result).isEqualTo(body.toUpperCase(Locale.US));
    }

    public static class Profile implements QuarkusTestProfile {
        private final String file;

        public Profile(String file) {
            this.file = file;
        }

        @Override
        public Map<String, String> getConfigOverrides() {
            String res = System.getProperty("camel.k.test.dir.resources", "./src/test/resources");

            return mapOf(
                "camel.k.sources[0].location", "file:" + res + "/listener.groovy",
                "camel.k.sources[1].location", "file:" + res + "/sources/" + file,
                "camel.k.sources[1].interceptors[0]", "knative-source"
            );
        }
    }
}
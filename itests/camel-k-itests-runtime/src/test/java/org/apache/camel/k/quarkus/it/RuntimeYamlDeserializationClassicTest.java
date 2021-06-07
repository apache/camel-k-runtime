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

import java.util.Map;

import javax.ws.rs.core.MediaType;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.apache.camel.util.CollectionHelper.mapOf;
import static org.assertj.core.api.Assertions.assertThat;

@TestProfile(RuntimeYamlDeserializationClassicTest.Profile.class)
@QuarkusTest
public class RuntimeYamlDeserializationClassicTest {
    @Test
    public void testContextCustomizerFromProperties() {
        JsonPath p = given()
            .accept(MediaType.APPLICATION_JSON)
            .get("/test/inspect")
            .then()
                .statusCode(200)
            .extract()
                .body()
                .jsonPath();

        assertThat(p.getMap("global-options")).containsEntry("CamelYamlDslDeserializationMode", "CLASSIC");
    }

    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return mapOf(
                "camel.k.yaml.deserialization-mode", "CLASSIC"
            );
        }
    }
}

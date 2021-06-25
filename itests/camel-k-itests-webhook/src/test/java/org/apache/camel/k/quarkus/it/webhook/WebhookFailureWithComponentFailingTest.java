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
package org.apache.camel.k.quarkus.it.webhook;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import org.apache.camel.component.webhook.WebhookAction;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.apache.camel.util.CollectionHelper.mapOf;

@TestProfile(WebhookFailureWithComponentFailingTest.Profile.class)
@QuarkusTest
public class WebhookFailureWithComponentFailingTest {
    @Test
    public void testFailure() {
        final String code = ""
            + "\n- from:"
            + "\n    uri: \"webhook:failing:test?foo=bar\""
            + "\n    steps:"
            + "\n      - to: \"log:webhook\""
            + "\n- from:"
            + "\n    uri: \"timer:tick\""
            + "\n    steps:"
            + "\n      - to: \"log:tick\"";

        given()
            .body(code)
        .when()
            .post("/test/load")
            .then()
            .statusCode(500);
    }

    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return mapOf(
                "camel.component.webhook.configuration.webhook-auto-register", "false",
                "camel.k.customizer.webhook.enabled", "true",
                "camel.k.customizer.webhook.action", WebhookAction.UNREGISTER.name()
            );
        }
    }
}

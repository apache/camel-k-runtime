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
import java.util.concurrent.TimeUnit;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import org.apache.camel.component.webhook.WebhookAction;
import org.assertj.core.util.Strings;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.when;
import static org.apache.camel.util.CollectionHelper.mapOf;
import static org.awaitility.Awaitility.await;

@TestProfile(WebhookRegisterTest.Profile.class)
@QuarkusTest
public class WebhookRegisterTest {
    private static final WebhookAction ACTION = WebhookAction.REGISTER;

    @Test
    public void testRegister() {
        await().atMost(10, TimeUnit.SECONDS).until(() -> {
            String result = when()
                .get("/test/counter/{action}", ACTION.name())
                .then()
                .statusCode(200)
                .extract().body().asString();

            return !Strings.isNullOrEmpty(result);
        });
    }

    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            String res = System.getProperty("camel.k.test.dir.resources", "./src/test/resources");

            return mapOf(
                "camel.component.webhook.configuration.webhook-auto-register", "false",
                "camel.k.customizer.webhook.enabled", "true",
                "camel.k.customizer.webhook.action", ACTION.name(),
                "camel.k.sources[0].location", "file:" + res + "/routes/registration_routes.yaml"
            );
        }
    }
}

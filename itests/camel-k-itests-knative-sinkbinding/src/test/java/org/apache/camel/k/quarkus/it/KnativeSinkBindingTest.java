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

import jakarta.ws.rs.core.MediaType;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import org.apache.camel.component.knative.spi.Knative;
import org.junit.jupiter.api.Test;

import static org.apache.camel.util.CollectionHelper.mapOf;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTestResource(KnativeSinkBindingTest.Resource.class)
@QuarkusTest
public class KnativeSinkBindingTest {
    @Test
    public void sinkbinding() {
        JsonPath p = RestAssured.given()
            .accept(MediaType.APPLICATION_JSON)
            .get("/test/customizers/sinkbinding")
            .then()
                .statusCode(200)
            .extract()
                .body()
                .jsonPath();

        assertThat(p.getString("name")).isEqualTo("mychannel");
        assertThat(p.getString("type")).isEqualTo(Knative.Type.channel.name());
        assertThat(p.getString("apiVersion")).isEqualTo("messaging.knative.dev/v1beta1");
        assertThat(p.getString("kind")).isEqualTo("InMemoryChannel");
    }

    @Test
    public void resources() {
        JsonPath p = RestAssured.given()
            .accept(MediaType.APPLICATION_JSON)
            .get("/test/resource/mychannel")
            .then()
                .statusCode(200)
            .extract()
                .body()
                .jsonPath();

        assertThat(p.getString("url")).isEqualTo("http://theurl");
        assertThat(p.getString("name")).isEqualTo("mychannel");
        assertThat(p.getString("type")).isEqualTo(Knative.Type.channel.name());
        assertThat(p.getString("apiVersion")).isEqualTo("messaging.knative.dev/v1beta1");
        assertThat(p.getString("kind")).isEqualTo("InMemoryChannel");
    }

    public static final class Resource implements QuarkusTestResourceLifecycleManager {
        @Override
        public Map<String, String> start() {
            return mapOf(
                "k.sink", "http://theurl",
                "camel.k.customizer.sinkbinding.enabled", "true",
                "camel.k.customizer.sinkbinding.name", "mychannel",
                "camel.k.customizer.sinkbinding.type", "channel",
                "camel.k.customizer.sinkbinding.kind", "InMemoryChannel",
                "camel.k.customizer.sinkbinding.api-version", "messaging.knative.dev/v1beta1"
            );
        }
        @Override
        public void stop() {

        }
    }
}
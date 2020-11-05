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

import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.apache.camel.util.CollectionHelper.mapOf;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@QuarkusTestResource(RuntimeWithInterceptorTest.Resources.class)
public class RuntimeWithInterceptorTest {
    @Test
    public void inspect() {
        List<String> endpoints = given()
            .accept(MediaType.APPLICATION_JSON)
            .get("/runtime/route-outputs/my-route")
            .then()
                .statusCode(200)
            .extract()
                .body()
                .jsonPath().getList(".", String.class);

        assertThat(endpoints).last().isEqualTo("knative://endpoint/sink");
    }

    public static class Resources implements QuarkusTestResourceLifecycleManager {
        @Override
        public Map<String, String> start() {
            final String res = System.getProperty("camel.k.test.dir.resources", ".");

            return mapOf(
                // sources
                "camel.k.sources[0].location", "file:" + res + "/routes-with-beans.yaml",
                "camel.k.sources[0].type", "source",
                "camel.k.sources[0].interceptors[0]", "knative-source"
            );
        }

        @Override
        public void stop() {
        }
    }
}
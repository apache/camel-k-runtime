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
package org.apache.camel.k.inspector;

import java.net.URL;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.k.Runtime;
import org.apache.camel.test.AvailablePortFinder;
import org.apache.camel.util.ObjectHelper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;

public class InspectorCustomizerTest {

    @ParameterizedTest
    @ValueSource(strings = { "", "/test", "/test/nested" })
    public void testInspectorConfigurer(String path) throws Exception {
        Runtime runtime = Runtime.on(new DefaultCamelContext());

        runtime.getRegistry().bind("my-customizer", (InspectorCustomizer) router ->
            router.route(HttpMethod.GET, "/my/path")
                .handler(routingContext -> {
                    JsonObject response = new JsonObject();
                    response.put("status", "UP");

                    routingContext.response()
                        .putHeader("content-type", "application/json")
                        .setStatusCode(200)
                        .end(Json.encodePrettily(response));
                })
        );

        InspectorContextCustomizer inspectorCustomizer = new InspectorContextCustomizer();
        inspectorCustomizer.setBindPort(AvailablePortFinder.getNextAvailable());

        String url;
        if (ObjectHelper.isEmpty(path)) {
            url = "http://localhost:" + inspectorCustomizer.getBindPort() + "/my/path";
        } else {
            inspectorCustomizer.setPath(path);

            url = "http://localhost:" + inspectorCustomizer.getBindPort() + path + "/my/path";
        }

        inspectorCustomizer.apply(runtime.getCamelContext());

        when()
            .get(new URL(url))
        .then()
            .statusCode(200)
            .body("status", equalTo("UP"));
    }
}

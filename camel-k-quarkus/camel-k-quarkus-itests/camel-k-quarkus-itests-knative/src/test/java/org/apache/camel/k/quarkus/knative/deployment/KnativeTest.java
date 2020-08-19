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
package org.apache.camel.k.quarkus.knative.deployment;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import javax.ws.rs.core.MediaType;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import org.apache.camel.Exchange;
import org.apache.camel.component.knative.spi.CloudEvent;
import org.apache.camel.component.knative.spi.CloudEvents;
import org.apache.camel.component.knative.spi.Knative;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class KnativeTest {
    @Test
    public void inspect() throws IOException {
        JsonPath p = RestAssured.given()
            .contentType(MediaType.TEXT_PLAIN)
            .accept(MediaType.APPLICATION_JSON)
            .get("/test/inspect")
            .then()
                .statusCode(200)
            .extract()
                .body()
                .jsonPath();

        assertThat(p.getMap("env-meta", String.class, String.class))
            .containsEntry(Knative.KNATIVE_EVENT_TYPE, "camel.k.evt")
            .containsEntry(Knative.SERVICE_META_PATH, "/knative")
            .containsEntry("camel.endpoint.kind", "source");
    }

    @Test
    public void invoke() {
        final CloudEvents ce = CloudEvents.v1_0;
        final String payload = "test";

        String result = given()
            .body(payload)
            .header(Exchange.CONTENT_TYPE, "text/plain")
            .header(ce.mandatoryAttribute(CloudEvent.CAMEL_CLOUD_EVENT_VERSION).http(), ce.v1_0.version())
            .header(ce.mandatoryAttribute(CloudEvent.CAMEL_CLOUD_EVENT_TYPE).http(), "org.apache.camel.event")
            .header(ce.mandatoryAttribute(CloudEvent.CAMEL_CLOUD_EVENT_ID).http(), "myEventID")
            .header(ce.mandatoryAttribute(CloudEvent.CAMEL_CLOUD_EVENT_TIME).http(), DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now()))
            .header(ce.mandatoryAttribute(CloudEvent.CAMEL_CLOUD_EVENT_SOURCE).http(), "/somewhere")
        .when()
            .post("/knative")
        .then()
            .statusCode(200)
            .extract()
            .asString();

        assertThat(result).isEqualTo(payload.toUpperCase());
    }
}

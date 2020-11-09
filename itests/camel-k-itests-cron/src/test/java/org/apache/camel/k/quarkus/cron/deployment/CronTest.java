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
package org.apache.camel.k.quarkus.cron.deployment;

import java.util.concurrent.TimeUnit;

import io.quarkus.test.junit.QuarkusTest;
import org.apache.camel.k.cron.CronSourceLoaderInterceptor;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.when;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;

@QuarkusTest
public class CronTest {
    @Test
    public void cronInterceptorIsRegistered() {
        when()
            .get("/test/find-cron-interceptor")
        .then()
            .statusCode(200)
            .body(is(CronSourceLoaderInterceptor.class.getName()));
    }

    @Test
    public void cronInvokesShutdown() {
        when()
            .get("/test/load")
            .then()
            .statusCode(200)
            .body(is("1"));

        await().atMost(10, TimeUnit.SECONDS).until(() -> {
            String result = when()
                .get("/test/stopped")
                .then()
                .statusCode(200)
                .extract().body().asString();

            return "true".equals(result);
        });
    }
}

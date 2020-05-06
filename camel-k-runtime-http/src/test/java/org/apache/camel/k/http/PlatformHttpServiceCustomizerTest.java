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
package org.apache.camel.k.http;

import java.net.ConnectException;
import java.util.Arrays;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.platform.http.PlatformHttpComponent;
import org.apache.camel.component.platform.http.PlatformHttpConstants;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.k.Runtime;
import org.apache.camel.k.http.engine.RuntimePlatformHttpEngine;
import org.apache.camel.k.test.AvailablePortFinder;
import org.apache.camel.support.jsse.KeyManagersParameters;
import org.apache.camel.support.jsse.KeyStoreParameters;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.support.jsse.SSLContextServerParameters;
import org.apache.camel.support.jsse.TrustManagersParameters;
import org.apache.camel.util.ObjectHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.Matchers.equalTo;

public class PlatformHttpServiceCustomizerTest {

    @ParameterizedTest
    @ValueSource(strings = { "", "/test", "/test/nested" })
    public void testPlatformHttpServiceCustomizer(String path) throws Exception {
        Runtime runtime = Runtime.on(new DefaultCamelContext());

        PlatformHttpServiceContextCustomizer httpService = new PlatformHttpServiceContextCustomizer();
        httpService.setBindPort(AvailablePortFinder.getNextAvailable());

        if (ObjectHelper.isNotEmpty(path)) {
            httpService.setPath(path);
        }

        httpService.apply(runtime.getCamelContext());

        try {
            runtime.getCamelContext().start();

            PlatformHttp.lookup(runtime.getCamelContext()).router().route(HttpMethod.GET, "/my/path")
                .handler(routingContext -> {
                    JsonObject response = new JsonObject();
                    response.put("status", "UP");

                    routingContext.response()
                        .putHeader("content-type", "application/json")
                        .setStatusCode(200)
                        .end(Json.encodePrettily(response));
                });

            given()
                .port(httpService.getBindPort())
            .when()
                .get(path + "/my/path")
            .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
        } finally {
            runtime.getCamelContext().stop();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "/", "/test", "/test/nested" })
    public void testPlatformHttpComponent(String path) throws Exception {
        Runtime runtime = Runtime.on(new DefaultCamelContext());
        runtime.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                fromF("platform-http:%s", path)
                    .setBody().constant(PlatformHttpConstants.PLATFORM_HTTP_COMPONENT_NAME);
            }
        });

        PlatformHttpServiceContextCustomizer httpService = new PlatformHttpServiceContextCustomizer();
        httpService.setBindPort(AvailablePortFinder.getNextAvailable());
        httpService.apply(runtime.getCamelContext());

        PlatformHttpComponent c = runtime.getCamelContext().getComponent(PlatformHttpConstants.PLATFORM_HTTP_COMPONENT_NAME, PlatformHttpComponent.class);

        assertThat(c).isNotNull();
        assertThat(c.getEngine()).isInstanceOf(RuntimePlatformHttpEngine.class);

        try {
            runtime.getCamelContext().start();

            given()
                .port(httpService.getBindPort())
            .when()
                .get(path)
            .then()
                .statusCode(200)
                .body(equalTo(PlatformHttpConstants.PLATFORM_HTTP_COMPONENT_NAME));
        } finally {
            runtime.getCamelContext().stop();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "/", "/test", "/test/nested" })
    public void testPlatformHttpComponentPost(String path) throws Exception {
        Runtime runtime = Runtime.on(new DefaultCamelContext());
        runtime.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                fromF("platform-http:%s", path)
                    .transform().body(String.class, b -> b.toUpperCase());
            }
        });

        PlatformHttpServiceContextCustomizer httpService = new PlatformHttpServiceContextCustomizer();
        httpService.setBindPort(AvailablePortFinder.getNextAvailable());
        httpService.apply(runtime.getCamelContext());

        PlatformHttpComponent c = runtime.getCamelContext().getComponent(PlatformHttpConstants.PLATFORM_HTTP_COMPONENT_NAME, PlatformHttpComponent.class);

        assertThat(c).isNotNull();
        assertThat(c.getEngine()).isInstanceOf(RuntimePlatformHttpEngine.class);

        try {
            runtime.getCamelContext().start();

            given()
                .port(httpService.getBindPort())
                .body("test")
            .when()
                .post(path)
            .then()
                .statusCode(200)
                .body(equalTo("TEST"));
        } finally {
            runtime.getCamelContext().stop();
        }
    }

    @Test
    public void testPlatformHttpServiceNotAvailableBeforeCamelContextStarts() throws Exception {
        Runtime runtime = Runtime.on(new DefaultCamelContext());

        var httpService = new PlatformHttpServiceContextCustomizer();
        httpService.setBindPort(AvailablePortFinder.getNextAvailable());
        httpService.apply(runtime.getCamelContext());

        PlatformHttp.lookup(runtime.getCamelContext())
            .router()
            .route(HttpMethod.GET, "/my/path")
            .handler(routingContext -> routingContext.response().setStatusCode(200).end());

        assertThatExceptionOfType(ConnectException.class).isThrownBy(
            () -> {
                given()
                    .port(httpService.getBindPort())
                .when()
                    .get("/my/path")
                .then()
                    .extract();
            }
        );
    }

    @Test
    public void testPlatformHttpComponentSSL() throws Exception {
        KeyStoreParameters keystoreParameters = new KeyStoreParameters();
        keystoreParameters.setResource("jsse/service.jks");
        keystoreParameters.setPassword("security");

        SSLContextParameters serviceSSLContextParameters = new SSLContextParameters();
        KeyManagersParameters serviceSSLKeyManagers = new KeyManagersParameters();
        serviceSSLKeyManagers.setKeyPassword("security");
        serviceSSLKeyManagers.setKeyStore(keystoreParameters);
        serviceSSLContextParameters.setKeyManagers(serviceSSLKeyManagers);

        KeyStoreParameters truststoreParameters = new KeyStoreParameters();
        truststoreParameters.setResource("jsse/truststore.jks");
        truststoreParameters.setPassword("storepass");

        TrustManagersParameters clientAuthServiceSSLTrustManagers = new TrustManagersParameters();
        clientAuthServiceSSLTrustManagers.setKeyStore(truststoreParameters);
        serviceSSLContextParameters.setTrustManagers(clientAuthServiceSSLTrustManagers);
        SSLContextServerParameters clientAuthSSLContextServerParameters = new SSLContextServerParameters();
        clientAuthSSLContextServerParameters.setClientAuthentication("REQUIRE");
        serviceSSLContextParameters.setServerParameters(clientAuthSSLContextServerParameters);

        SSLContextParameters clientSSLContextParameters = new SSLContextParameters();
        TrustManagersParameters clientSSLTrustManagers = new TrustManagersParameters();
        clientSSLTrustManagers.setKeyStore(truststoreParameters);
        clientSSLContextParameters.setTrustManagers(clientSSLTrustManagers);

        KeyManagersParameters clientAuthClientSSLKeyManagers = new KeyManagersParameters();
        clientAuthClientSSLKeyManagers.setKeyPassword("security");
        clientAuthClientSSLKeyManagers.setKeyStore(keystoreParameters);
        clientSSLContextParameters.setKeyManagers(clientAuthClientSSLKeyManagers);

        CamelContext context = new DefaultCamelContext();
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                fromF("platform-http:/")
                    .transform().body(String.class, b -> b.toUpperCase());
            }
        });

        PlatformHttpServiceContextCustomizer httpService = new PlatformHttpServiceContextCustomizer();
        httpService.setBindPort(AvailablePortFinder.getNextAvailable());
        httpService.setSslContextParameters(serviceSSLContextParameters);
        httpService.apply(context);

        try {
            context.getRegistry().bind("clientSSLContextParameters", clientSSLContextParameters);
            context.start();

            String result = context.createFluentProducerTemplate()
                .toF("https://localhost:%d?sslContextParameters=#clientSSLContextParameters", httpService.getBindPort())
                .withBody("test")
                .request(String.class);

            assertThat(result).isEqualTo("TEST");
        } finally {
            context.stop();
        }
    }

    @Test
    public void testPlatformHttpComponentGlobalSSL() throws Exception {
        KeyStoreParameters keystoreParameters = new KeyStoreParameters();
        keystoreParameters.setResource("jsse/service.jks");
        keystoreParameters.setPassword("security");

        SSLContextParameters serviceSSLContextParameters = new SSLContextParameters();
        KeyManagersParameters serviceSSLKeyManagers = new KeyManagersParameters();
        serviceSSLKeyManagers.setKeyPassword("security");
        serviceSSLKeyManagers.setKeyStore(keystoreParameters);
        serviceSSLContextParameters.setKeyManagers(serviceSSLKeyManagers);

        KeyStoreParameters truststoreParameters = new KeyStoreParameters();
        truststoreParameters.setResource("jsse/truststore.jks");
        truststoreParameters.setPassword("storepass");

        TrustManagersParameters clientAuthServiceSSLTrustManagers = new TrustManagersParameters();
        clientAuthServiceSSLTrustManagers.setKeyStore(truststoreParameters);
        serviceSSLContextParameters.setTrustManagers(clientAuthServiceSSLTrustManagers);
        SSLContextServerParameters clientAuthSSLContextServerParameters = new SSLContextServerParameters();
        clientAuthSSLContextServerParameters.setClientAuthentication("REQUIRE");
        serviceSSLContextParameters.setServerParameters(clientAuthSSLContextServerParameters);

        SSLContextParameters clientSSLContextParameters = new SSLContextParameters();
        TrustManagersParameters clientSSLTrustManagers = new TrustManagersParameters();
        clientSSLTrustManagers.setKeyStore(truststoreParameters);
        clientSSLContextParameters.setTrustManagers(clientSSLTrustManagers);

        KeyManagersParameters clientAuthClientSSLKeyManagers = new KeyManagersParameters();
        clientAuthClientSSLKeyManagers.setKeyPassword("security");
        clientAuthClientSSLKeyManagers.setKeyStore(keystoreParameters);
        clientSSLContextParameters.setKeyManagers(clientAuthClientSSLKeyManagers);

        CamelContext context = new DefaultCamelContext();
        context.setSSLContextParameters(serviceSSLContextParameters);
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                fromF("platform-http:/")
                    .transform().body(String.class, b -> b.toUpperCase());
            }
        });

        PlatformHttpServiceContextCustomizer httpService = new PlatformHttpServiceContextCustomizer();
        httpService.setBindPort(AvailablePortFinder.getNextAvailable());
        httpService.setUseGlobalSslContextParameters(true);
        httpService.apply(context);

        try {
            context.getRegistry().bind("clientSSLContextParameters", clientSSLContextParameters);
            context.start();

            String result = context.createFluentProducerTemplate()
                .toF("https://localhost:%d?sslContextParameters=#clientSSLContextParameters", httpService.getBindPort())
                .withBody("test")
                .request(String.class);

            assertThat(result).isEqualTo("TEST");
        } finally {
            context.stop();
        }
    }

    @Test
    public void testPlatformHttpComponentCORS() throws Exception {
        CamelContext context = new DefaultCamelContext();
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                fromF("platform-http:/")
                    .transform().constant("cors");
            }
        });

        PlatformHttpServiceContextCustomizer httpService = new PlatformHttpServiceContextCustomizer();
        httpService.setBindPort(AvailablePortFinder.getNextAvailable());
        httpService.getCors().setEnabled(true);
        httpService.getCors().setMethods(Arrays.asList("GET", "POST"));
        httpService.apply(context);

        try {
            context.start();

            String origin = "http://custom.origin.quarkus";
            String methods = "GET,POST";
            String headers = "X-Custom";

            given()
                .port(httpService.getBindPort())
                .header("Origin", origin)
                .header("Access-Control-Request-Method", methods)
                .header("Access-Control-Request-Headers", headers)
            .when()
                .get("/")
            .then()
                .statusCode(200)
                .header("Access-Control-Allow-Origin", origin)
                .header("Access-Control-Allow-Methods", methods)
                .header("Access-Control-Allow-Headers", headers);
        } finally {
            context.stop();
        }
    }
}

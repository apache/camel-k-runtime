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
package org.apache.camel.k.webhook;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.camel.CamelContext;
import org.apache.camel.Consumer;
import org.apache.camel.FailedToCreateRouteException;
import org.apache.camel.NamedNode;
import org.apache.camel.Route;
import org.apache.camel.k.listener.ContextConfigurer;
import org.apache.camel.k.main.ApplicationRuntime;
import org.apache.camel.k.support.SourcesSupport;
import org.apache.camel.spi.RoutePolicy;
import org.apache.camel.spi.RoutePolicyFactory;
import org.apache.camel.support.RoutePolicySupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

public class WebhookTest {
    @ParameterizedTest
    @EnumSource(WebhookAction.class)
    public void testWebhookRegistration(WebhookAction action) throws Exception {
        ApplicationRuntime runtime = new ApplicationRuntime();
        runtime.setProperties(
            "camel.component.webhook.configuration.webhook-auto-register", "false",
            "camel.k.customizer.webhook.enabled", "true",
            "camel.k.customizer.webhook.action", action.name().toLowerCase());

        CountDownLatch operation = new CountDownLatch(1);
        Map<WebhookAction, AtomicInteger> registerCounters = new HashMap<>();
        Arrays.stream(WebhookAction.values()).forEach(v -> registerCounters.put(v, new AtomicInteger()));

        runtime.getCamelContext().addComponent(
            "dummy",
            new DummyWebhookComponent(
            () -> {
                registerCounters.get(WebhookAction.REGISTER).incrementAndGet();
                operation.countDown();
            },
            () -> {
                registerCounters.get(WebhookAction.UNREGISTER).incrementAndGet();
                operation.countDown();
            })
        );

        AtomicBoolean routeStarted = new AtomicBoolean();
        runtime.addListener(new ContextConfigurer());
        runtime.addListener(SourcesSupport.forRoutes("classpath:webhook.js"));
        runtime.getCamelContext().addRoutePolicyFactory(new RoutePolicyFactory() {
            @Override
            public RoutePolicy createRoutePolicy(CamelContext camelContext, String routeId, NamedNode route) {
                return new RoutePolicySupport() {
                    @Override
                    public void onStart(Route route) {
                        routeStarted.set(true);
                        super.onStart(route);
                    }

                    @Override
                    public void startRoute(Route route) throws Exception {
                        routeStarted.set(true);
                        super.startRoute(route);
                    }

                    @Override
                    public void startConsumer(Consumer consumer) throws Exception {
                        routeStarted.set(true);
                        super.startConsumer(consumer);
                    }
                };
            }
        });

        runtime.run();

        operation.await(15, TimeUnit.SECONDS);
        for (WebhookAction a : WebhookAction.values()) {
            if (a == action) {
                assertThat(registerCounters.get(a)).hasValue(1);
            } else {
                assertThat(registerCounters.get(a)).hasValue(0);
            }
        }

        assertThat(routeStarted).isFalse();
    }

    @ParameterizedTest()
    @EnumSource(WebhookAction.class)
    public void testRegistrationFailure(WebhookAction action) throws Exception {
        ApplicationRuntime runtime = new ApplicationRuntime();
        runtime.setProperties(
            "camel.component.webhook.configuration.webhook-auto-register", "false",
            "camel.k.customizer.webhook.enabled", "true",
            "camel.k.customizer.webhook.action", action.name());

        runtime.getCamelContext().addComponent(
            "dummy",
            new DummyWebhookComponent(
            () -> {
                throw new RuntimeException("dummy error");
            },
            () -> {
                throw new RuntimeException("dummy error");
            })
        );

        runtime.addListener(new ContextConfigurer());
        runtime.addListener(SourcesSupport.forRoutes("classpath:webhook.js"));

        Assertions.assertThrows(FailedToCreateRouteException.class, runtime::run);
    }

    @Test
    public void testAutoRegistrationNotDisabled() throws Exception {
        ApplicationRuntime runtime = new ApplicationRuntime();
        runtime.setProperties(
            "camel.k.customizer.webhook.enabled", "true",
            "camel.k.customizer.webhook.action", WebhookAction.REGISTER.name());

        runtime.getCamelContext().addComponent("dummy", new DummyWebhookComponent());
        runtime.addListener(new ContextConfigurer());
        runtime.addListener(SourcesSupport.forRoutes("classpath:webhook.js"));

        Assertions.assertThrows(FailedToCreateRouteException.class, runtime::run);
    }

}

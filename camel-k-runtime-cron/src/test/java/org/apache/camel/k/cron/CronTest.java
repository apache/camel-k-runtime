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
package org.apache.camel.k.cron;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.camel.CamelContext;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.k.listener.ContextConfigurer;
import org.apache.camel.k.listener.RoutesConfigurer;
import org.apache.camel.k.loader.js.JavaScriptSourceLoader;
import org.apache.camel.k.main.ApplicationRuntime;
import org.apache.camel.support.LifecycleStrategySupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

public class CronTest {

    @ParameterizedTest
    @MethodSource("parameters")
    public void testCronTimerActivation(String routes, String customizer) throws Exception {
        ApplicationRuntime runtime = new ApplicationRuntime();
        runtime.addListener(RoutesConfigurer.forRoutes(routes));
        runtime.addListener(new ContextConfigurer());

        Properties properties = new Properties();
        properties.setProperty("customizer." + customizer + ".enabled", "true");
        runtime.setProperties(properties);

        // To check auto-termination of Camel context
        CountDownLatch termination = new CountDownLatch(1);
        runtime.getCamelContext().addLifecycleStrategy(new LifecycleStrategySupport() {
            @Override
            public void onContextStop(CamelContext context) {
                termination.countDown();
            }
        });

        MockEndpoint mock = runtime.getCamelContext().getEndpoint("mock:result", MockEndpoint.class);
        mock.expectedMessageCount(1);
        mock.setResultWaitTime(10000);

        runtime.run();
        mock.assertIsSatisfied();

        termination.await(10, TimeUnit.SECONDS);
        assertThat(termination.getCount()).isEqualTo(0);
    }

    static Stream<Arguments> parameters() {
        return Stream.of(
            Arguments.arguments("classpath:routes-timer.js", "cron-timer"),
            Arguments.arguments("classpath:routes-quartz.js", "cron-quartz")
        );
    }

}

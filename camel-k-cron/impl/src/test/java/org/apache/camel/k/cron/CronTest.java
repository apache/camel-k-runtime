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

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.k.Runtime;
import org.apache.camel.k.Source;
import org.apache.camel.k.SourceLoader;
import org.apache.camel.k.loader.yaml.YamlSourceLoader;
import org.apache.camel.k.support.Sources;
import org.apache.camel.support.LifecycleStrategySupport;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

public class CronTest {
    @ParameterizedTest
    @MethodSource("parameters")
    public void testCronTimerActivation(String code, String cronOverride) throws Exception {
        final Runtime runtime = Runtime.on(new DefaultCamelContext());
        final SourceLoader loader = new YamlSourceLoader();
        final Source source = Sources.fromBytes("my-cron", "yaml", null, List.of("cron"), code.getBytes(StandardCharsets.UTF_8));

        final CronSourceLoaderInterceptor interceptor = new CronSourceLoaderInterceptor();
        interceptor.setRuntime(runtime);
        interceptor.setOverridableComponents(cronOverride);

        RoutesBuilder builder = interceptor.afterLoad(
            loader,
            source,
            loader.load(runtime, source));

        runtime.getCamelContext().addRoutes(builder);

        CountDownLatch termination = new CountDownLatch(1);
        runtime.getCamelContext().addLifecycleStrategy(new LifecycleStrategySupport() {
            @Override
            public void onContextStopped(CamelContext context) {
                termination.countDown();
            }
        });

        MockEndpoint mock = runtime.getCamelContext().getEndpoint("mock:result", MockEndpoint.class);
        mock.expectedMessageCount(1);
        mock.setResultWaitTime(10000);

        runtime.getCamelContext().start();
        mock.assertIsSatisfied();

        termination.await(10, TimeUnit.SECONDS);
        assertThat(termination.getCount()).isEqualTo(0);
    }

    static Stream<Arguments> parameters() {
        return Stream.of(
            Arguments.arguments(
                ""
                + "\n- from:"
                + "\n    uri: \"timer:tick?period=1&delay=600000\""
                + "\n    steps:"
                + "\n      - to: \"mock:result\"",
                "timer"),
            Arguments.arguments(
                ""
                + "\n- from:"
                + "\n    uri: \"quartz:tick?cron=0 0 0 * * ? 2099\""
                + "\n    steps:"
                + "\n      - to: \"mock:result\"",
                "quartz"),
            Arguments.arguments(
                ""
                + "\n- from:"
                + "\n    uri: \"quartz:tick?cron=0 0 0 * * ? 2099\""
                + "\n    steps:"
                + "\n      - to: \"mock:result\"",
                "timer,quartz")
        );
    }

}

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
package org.apache.camel.k.tracing;

import java.util.Random;

import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.k.Runtime;
import org.apache.camel.k.support.RuntimeSupport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TracingContextCustomizerTest {
    @Test
    public void testTracingConfiguration() {
        final String endpoint = "http://jaeger:14268/api/traces";
        final String type = "const";
        final int param = new Random().nextInt(10);

        Runtime runtime = Runtime.on(new DefaultCamelContext());
        runtime.setProperties(
            "camel.k.customizer.tracing.enabled", "true",
            "camel.k.customizer.tracing.reporter.sender.endpoint", endpoint,
            "camel.k.customizer.tracing.sampler.type", type,
            "camel.k.customizer.tracing.sampler.param", Integer.toString(param));

        assertThat(RuntimeSupport.configureContextCustomizers(runtime))
            .hasOnlyOneElementSatisfying(customizer -> {
                assertThat(customizer)
                    .isInstanceOfSatisfying(TracingContextCustomizer.class, tracing -> {
                        assertThat(tracing.getReporter().getSenderConfiguration().getEndpoint()).isEqualTo(endpoint);
                        assertThat(tracing.getSampler().getType()).isEqualTo(type);
                        assertThat(tracing.getSampler().getParam().intValue()).isEqualTo(param);
                    });
            });
    }
}

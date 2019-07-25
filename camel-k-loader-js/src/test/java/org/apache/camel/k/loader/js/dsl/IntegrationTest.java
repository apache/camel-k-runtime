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
package org.apache.camel.k.loader.js.dsl;

import org.apache.camel.component.seda.SedaComponent;
import org.apache.camel.k.Runtime;
import org.apache.camel.k.listener.RoutesConfigurer;
import org.apache.camel.k.main.ApplicationRuntime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class IntegrationTest {
    @Test
    public void testComponentConfiguration() throws Exception {
        ApplicationRuntime runtime = new ApplicationRuntime();
        runtime.addListener(RoutesConfigurer.forRoutes("classpath:routes-with-component-configuration.js"));
        runtime.addListener(Runtime.Phase.Started, r -> {
            SedaComponent seda = r.getCamelContext().getComponent("seda", SedaComponent.class);

            assertThat(seda).isNotNull();
            assertThat(seda).hasFieldOrPropertyWithValue("queueSize", 1234);

            runtime.stop();
        });

        runtime.run();
    }
}

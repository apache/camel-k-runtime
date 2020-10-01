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
package org.apache.camel.component.knative.spi;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

public class KnativeEnvironmentTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "{\"services\":[{\"type\":\"endpoint\",\"name\":\"knative3\",\"metadata\":{\"camel.endpoint.kind\":\"source\",\"knative.apiVersion\":\"serving.knative.dev/v1\",\"knative.kind\":\"Service\",\"service.path\":\"/\"}}]}",
        "{\"resources\":[{\"type\":\"endpoint\",\"name\":\"knative3\",\"metadata\":{\"camel.endpoint.kind\":\"source\",\"knative.apiVersion\":\"serving.knative.dev/v1\",\"knative.kind\":\"Service\",\"service.path\":\"/\"}}]}"
    })
    public void testKnativeEnvironmentDeserializationFromString(String content) throws Exception {
        CamelContext context = new DefaultCamelContext();
        KnativeEnvironment env = KnativeEnvironment.mandatoryLoadFromSerializedString(context, content);

        assertThat(env.lookup(Knative.Type.endpoint, "knative3"))
            .first()
                .hasFieldOrPropertyWithValue("url", null)
                .hasFieldOrProperty("metadata");
    }
}

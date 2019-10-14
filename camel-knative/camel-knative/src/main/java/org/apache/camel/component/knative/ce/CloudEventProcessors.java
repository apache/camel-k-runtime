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
package org.apache.camel.component.knative.ce;

import java.util.Objects;

import org.apache.camel.Processor;
import org.apache.camel.component.knative.KnativeEndpoint;
import org.apache.camel.component.knative.spi.CloudEvent;
import org.apache.camel.component.knative.spi.KnativeEnvironment;

public enum CloudEventProcessors implements CloudEventProcessor {
    V01(new CloudEventV01Processor()),
    V02(new CloudEventV02Processor());

    private final CloudEventProcessor instance;

    CloudEventProcessors(CloudEventProcessor instance) {
        this.instance = instance;
    }

    @Override
    public CloudEvent cloudEvent() {
        return instance.cloudEvent();
    }

    @Override
    public Processor consumer(KnativeEndpoint endpoint, KnativeEnvironment.KnativeServiceDefinition service) {
        return instance.consumer(endpoint, service);
    }

    @Override
    public Processor producer(KnativeEndpoint endpoint, KnativeEnvironment.KnativeServiceDefinition service) {
        return instance.producer(endpoint, service);
    }

    public static CloudEventProcessor fromSpecVersion(String version) {
        for (CloudEventProcessor processor: CloudEventProcessors.values()) {
            if (Objects.equals(processor.cloudEvent().version(), version)) {
                return processor;
            }
        }

        throw new IllegalArgumentException("Unable to find an implementation fo CloudEvents spec: " + version);
    }
}


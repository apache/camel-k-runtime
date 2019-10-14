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

import java.util.Objects;

public enum CloudEvents implements CloudEvent {
    V01(new CloudEventV01()),
    V02(new CloudEventV02());

    private final CloudEvent instance;

    CloudEvents(CloudEvent instance) {
        this.instance = instance;
    }

    @Override
    public String version() {
        return instance.version();
    }

    @Override
    public Attributes attributes() {
        return instance.attributes();
    }

    public static CloudEvent fromSpecVersion(String version) {
        for (CloudEvent event: CloudEvents.values()) {
            if (Objects.equals(event.version(), version)) {
                return event;
            }
        }

        throw new IllegalArgumentException("Unable to find an implementation fo CloudEvents spec: " + version);
    }
}


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

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

public enum CloudEvents implements CloudEvent {
    //
    // V0.1 - https://github.com/cloudevents/spec/blob/v0.1/spec.md
    //
    V01(new CloudEventImpl(
        "0.1",
        Arrays.asList(
            Attribute.simple("type", "CE-EventType", "eventType"),
            Attribute.simple("type.version", "CE-EventTypeVersion", "eventTypeVersion"),
            Attribute.simple("version", "CE-CloudEventsVersion", "cloudEventsVersion"),
            Attribute.simple("source", "CE-Source", "source"),
            Attribute.simple("id", "CE-EventID", "eventID"),
            Attribute.simple("time", "CE-EventTime", "eventTime"),
            Attribute.simple("schema.url", "CE-SchemaURL", "schemaURL"),
            Attribute.simple("content.type", "ContentType", "contentType"),
            Attribute.simple("extensions", "CE-Extensions", "extensions")
        )
    )),
    //
    // V0.2 - https://github.com/cloudevents/spec/blob/v0.2/spec.md
    //
    V02(new CloudEventImpl(
        "0.2",
        Arrays.asList(
            Attribute.simple("type", "ce-type", "type"),
            Attribute.simple("version", "ce-specversion", "specversion"),
            Attribute.simple("source", "ce-source", "source"),
            Attribute.simple("id", "ce-id", "id"),
            Attribute.simple("time", "ce-time", "time"),
            Attribute.simple("schema.url", "ce-schemaurl", "schemaurl"),
            Attribute.simple("content.type", "Content-Type", "contenttype")
        )
    )),
    //
    // V0.3 - https://github.com/cloudevents/spec/blob/v0.3/spec.md
    //
    V03(new CloudEventImpl(
        "0.3",
        Arrays.asList(
            Attribute.simple("id", "ce-id", "id"),
            Attribute.simple("source", "ce-source", "source"),
            Attribute.simple("version", "ce-specversion", "specversion"),
            Attribute.simple("type", "ce-type", "type"),
            Attribute.simple("data.content.encoding", "ce-datacontentencoding", "datacontentencoding"),
            Attribute.simple("data.content.type", "ce-datacontenttype", "datacontenttype"),
            Attribute.simple("schema.url", "ce-schemaurl", "schemaurl"),
            Attribute.simple("subject", "ce-subject", "subject"),
            Attribute.simple("time", "ce-time", "time")
        )
    ));

    private final CloudEvent instance;

    CloudEvents(CloudEvent instance) {
        this.instance = instance;
    }

    @Override
    public String version() {
        return instance.version();
    }

    @Override
    public Collection<Attribute> attributes() {
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

    private static class CloudEventImpl implements CloudEvent {
        private final String version;
        private final Collection<Attribute> attributes;

        public CloudEventImpl(String version, Collection<Attribute> attributes) {
            this.version = version;
            this.attributes = attributes;
        }

        @Override
        public String version() {
            return version;
        }

        @Override
        public Collection<Attribute> attributes() {
            return attributes;
        }
    }
}


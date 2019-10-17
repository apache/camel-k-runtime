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

import java.util.Map;
import java.util.Objects;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.component.knative.KnativeEndpoint;
import org.apache.camel.component.knative.spi.CloudEvent;
import org.apache.camel.component.knative.spi.CloudEvents;
import org.apache.camel.component.knative.spi.KnativeEnvironment;
import org.apache.commons.lang3.StringUtils;

import static org.apache.camel.util.ObjectHelper.ifNotEmpty;

public enum CloudEventProcessors implements CloudEventProcessor {
    V01(new AbstractCloudEventProcessor(CloudEvents.V01) {
        @SuppressWarnings("unchecked")
        @Override
        protected void decodeStructuredContent(Exchange exchange, Map<String, Object> content) {
            final CloudEvent ce = cloudEvent();
            final Message message = exchange.getIn();

            // body
            ifNotEmpty(content.remove("data"), message::setBody);

            ifNotEmpty(content.remove(ce.mandatoryAttribute("content.type").json()), val -> {
                message.setHeader(Exchange.CONTENT_TYPE, val);
            });

            //
            // Map extensions to standard camel headers
            //
            ifNotEmpty(content.remove("extensions"), val -> {
                if (val instanceof Map) {
                    ((Map<String, Object>) val).forEach(message::setHeader);
                }
            });

            for (CloudEvent.Attribute attribute: ce.attributes()) {
                ifNotEmpty(content.remove(attribute.json()), val -> {
                    message.setHeader(attribute.id(), val);
                });
            }
        }
    }),
    V02(new AbstractCloudEventProcessor(CloudEvents.V02) {
        @Override
        protected void decodeStructuredContent(Exchange exchange, Map<String, Object> content) {
            final CloudEvent ce = cloudEvent();
            final Message message = exchange.getIn();

            // body
            ifNotEmpty(content.remove("data"), message::setBody);

            ifNotEmpty(content.remove(ce.mandatoryAttribute("content.type").json()), val -> {
                message.setHeader(Exchange.CONTENT_TYPE, val);
            });

            for (CloudEvent.Attribute attribute: ce.attributes()) {
                ifNotEmpty(content.remove(attribute.json()), val -> {
                    message.setHeader(attribute.id(), val);
                });
            }

            //
            // Map every remaining field as it is (extensions).
            //
            content.forEach((key, val) -> {
                message.setHeader(StringUtils.lowerCase(key), val);
            });

        }
    }),
    V03(new AbstractCloudEventProcessor(CloudEvents.V03) {
        @Override
        protected void decodeStructuredContent(Exchange exchange, Map<String, Object> content) {
            final CloudEvent ce = cloudEvent();
            final Message message = exchange.getIn();

            // body
            ifNotEmpty(content.remove("data"), message::setBody);

            ifNotEmpty(content.remove(ce.mandatoryAttribute("data.content.type").json()), val -> {
                message.setHeader(Exchange.CONTENT_TYPE, val);
            });
            ifNotEmpty(content.remove(ce.mandatoryAttribute("data.content.encoding").json()), val -> {
                message.setBody(val);
            });

            for (CloudEvent.Attribute attribute: ce.attributes()) {
                ifNotEmpty(content.remove(attribute.json()), val -> {
                    message.setHeader(attribute.id(), val);
                });
            }

            //
            // Map every remaining field as it is (extensions).
            //
            content.forEach((key, val) -> {
                message.setHeader(StringUtils.lowerCase(key), val);
            });
        }
    });

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


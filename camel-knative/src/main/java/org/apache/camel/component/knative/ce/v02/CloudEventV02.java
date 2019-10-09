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
package org.apache.camel.component.knative.ce.v02;

import java.io.InputStream;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.component.knative.Knative;
import org.apache.camel.component.knative.KnativeEndpoint;
import org.apache.camel.component.knative.KnativeEnvironment;
import org.apache.camel.component.knative.KnativeSupport;
import org.apache.camel.component.knative.ce.CloudEvent;
import org.apache.commons.lang3.StringUtils;

import static org.apache.camel.util.ObjectHelper.ifNotEmpty;

public final class CloudEventV02 implements CloudEvent {
    public static final String VERSION = "0.2";
    public static final Attributes ATTRIBUTES = new Attributes() {
        @Override
        public String id() {
            return "ce-id";
        }

        @Override
        public String source() {
            return "ce-source";
        }

        @Override
        public String spec() {
            return "ce-specversion";
        }

        @Override
        public String type() {
            return "ce-type";
        }

        @Override
        public String time() {
            return "ce-time";
        }
    };

    @Override
    public String version() {
        return VERSION;
    }

    @Override
    public Attributes attributes() {
        return ATTRIBUTES;
    }

    @Override
    public Processor producer(KnativeEndpoint endpoint) {
        KnativeEnvironment.KnativeServiceDefinition service = endpoint.getService();
        String uri = endpoint.getEndpointUri();

        return exchange -> {
            String eventType = service.getMetadata().get(Knative.KNATIVE_EVENT_TYPE);
            if (eventType == null) {
                eventType = endpoint.getConfiguration().getCloudEventsType();
            }

            final String contentType = service.getMetadata().get(Knative.CONTENT_TYPE);
            final ZonedDateTime created = exchange.getCreated().toInstant().atZone(ZoneId.systemDefault());
            final String eventTime = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(created);
            final Map<String, Object> headers = exchange.getIn().getHeaders();

            headers.putIfAbsent(ATTRIBUTES.id(), exchange.getExchangeId());
            headers.putIfAbsent(ATTRIBUTES.source(), uri);
            headers.putIfAbsent(ATTRIBUTES.spec(), VERSION);
            headers.putIfAbsent(ATTRIBUTES.type(), eventType);
            headers.putIfAbsent(ATTRIBUTES.time(), eventTime);
            headers.putIfAbsent(Exchange.CONTENT_TYPE, contentType);

            // Always remove host so it's always computed from the URL and not inherited from the exchange
            headers.remove("Host");
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    public Processor consumer(KnativeEndpoint endpoint) {
        return exchange -> {
            if (!KnativeSupport.hasStructuredContent(exchange)) {
                //
                // The event is not in the form of Structured Content Mode
                // then leave it as it is.
                //
                // Note that this is true for http binding only.
                //
                // More info:
                //
                //   https://github.com/cloudevents/spec/blob/master/http-transport-binding.md#32-structured-content-mode
                //
                return;
            }

            try (InputStream is = exchange.getIn().getBody(InputStream.class)) {
                final Message message = exchange.getIn();
                final Map<String, Object> ce = Knative.MAPPER.readValue(is, Map.class);

                ifNotEmpty(ce.remove("contenttype"), val -> message.setHeader(Exchange.CONTENT_TYPE, val));
                ifNotEmpty(ce.remove("data"), val -> message.setBody(val));

                //
                // Map extensions to standard camel headers
                //
                ifNotEmpty(ce.remove("extensions"), val -> {
                    if (val instanceof Map) {
                        ((Map<String, Object>) val).forEach(message::setHeader);
                    }
                });

                ce.forEach((key, val) -> {
                    message.setHeader("ce-" + StringUtils.lowerCase(key), val);
                });
            }
        };
    }
}

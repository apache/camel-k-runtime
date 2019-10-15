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

import java.io.InputStream;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.knative.KnativeEndpoint;
import org.apache.camel.component.knative.spi.CloudEvent;
import org.apache.camel.component.knative.spi.Knative;
import org.apache.camel.component.knative.spi.KnativeEnvironment;

abstract class AbstractCloudEventProcessor implements CloudEventProcessor {
    private final CloudEvent cloudEvent;

    protected AbstractCloudEventProcessor(CloudEvent cloudEvent) {
        this.cloudEvent = cloudEvent;
    }

    @Override
    public CloudEvent cloudEvent() {
        return cloudEvent;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Processor consumer(KnativeEndpoint endpoint, KnativeEnvironment.KnativeServiceDefinition service) {
        return exchange -> {
            if (Objects.equals(exchange.getIn().getHeader(Exchange.CONTENT_TYPE), Knative.MIME_BATCH_CONTENT_MODE)) {
                throw new UnsupportedOperationException("Batched CloudEvents are not yet supported");
            }

            if (!Objects.equals(exchange.getIn().getHeader(Exchange.CONTENT_TYPE), Knative.MIME_STRUCTURED_CONTENT_MODE)) {
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
                decodeStructuredContent(exchange, Knative.MAPPER.readValue(is, Map.class));
            }
        };
    }

    protected abstract void decodeStructuredContent(Exchange exchange, Map<String, Object> content);

    @Override
    public Processor producer(KnativeEndpoint endpoint, KnativeEnvironment.KnativeServiceDefinition service) {
        final CloudEvent ce = cloudEvent();

        return exchange -> {
            String eventType = service.getMetadata().get(Knative.KNATIVE_EVENT_TYPE);
            if (eventType == null) {
                eventType = endpoint.getConfiguration().getCloudEventsType();
            }

            final String contentType = service.getMetadata().get(Knative.CONTENT_TYPE);
            final ZonedDateTime created = exchange.getCreated().toInstant().atZone(ZoneId.systemDefault());
            final String eventTime = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(created);
            final Map<String, Object> headers = exchange.getIn().getHeaders();

            headers.putIfAbsent(ce.mandatoryAttribute("id").id(), exchange.getExchangeId());
            headers.putIfAbsent(ce.mandatoryAttribute("source").id(), endpoint.getEndpointUri());
            headers.putIfAbsent(ce.mandatoryAttribute("version").id(), ce.version());
            headers.putIfAbsent(ce.mandatoryAttribute("type").id(), eventType);
            headers.putIfAbsent(ce.mandatoryAttribute("time").id(), eventTime);
            headers.putIfAbsent(Exchange.CONTENT_TYPE, contentType);
        };
    }
}

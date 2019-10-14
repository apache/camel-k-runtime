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
package org.apache.camel.component.knative;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.component.knative.ce.CloudEventProcessor;
import org.apache.camel.component.knative.ce.CloudEventProcessors;
import org.apache.camel.component.knative.spi.Knative;
import org.apache.camel.component.knative.spi.KnativeEnvironment;
import org.apache.camel.component.knative.spi.KnativeTransport;
import org.apache.camel.processor.Pipeline;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;
import org.apache.camel.support.DefaultEndpoint;
import org.apache.camel.support.PropertyBindingSupport;


@UriEndpoint(
    firstVersion = "3.0.0",
    scheme = "knative",
    syntax = "knative:type/name",
    title = "Knative",
    label = "cloud,eventing")
public class KnativeEndpoint extends DefaultEndpoint {
    @UriPath(description = "The Knative type")
    private final Knative.Type type;
    @UriPath(description = "The Knative name")
    private final String name;

    @UriParam
    private KnativeConfiguration configuration;

    private final KnativeTransport transport;
    private final CloudEventProcessor cloudEvent;

    public KnativeEndpoint(String uri, KnativeComponent component, Knative.Type type, String name, KnativeTransport transport, KnativeConfiguration configuration) {
        super(uri, component);

        this.type = type;
        this.name = name;
        this.transport = transport;
        this.configuration = configuration;
        this.cloudEvent = CloudEventProcessors.fromSpecVersion(configuration.getCloudEventsSpecVersion());
    }

    @Override
    public KnativeComponent getComponent() {
        return (KnativeComponent) super.getComponent();
    }

    @Override
    public Producer createProducer() throws Exception {
        final KnativeEnvironment.KnativeServiceDefinition service = lookupServiceDefinition(Knative.EndpointKind.sink);
        final Processor ceProcessor = cloudEvent.producer(this, service);
        final Processor ceConverter = new KnativeConversionProcessor(configuration.isJsonSerializationEnabled());
        final Producer producer = transport.createProducer(this, service);

        PropertyBindingSupport.build()
            .withCamelContext(getCamelContext())
            .withProperties(configuration.getTransportOptions())
            .withRemoveParameters(false)
            .withTarget(producer)
            .bind();

        return new KnativeProducer(this, ceProcessor, ceConverter, producer);
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        final KnativeEnvironment.KnativeServiceDefinition service = lookupServiceDefinition(Knative.EndpointKind.source);
        final Processor ceProcessor = cloudEvent.consumer(this, service);
        final Processor pipeline = Pipeline.newInstance(getCamelContext(), ceProcessor, processor);
        final Consumer consumer = transport.createConsumer(this, service, pipeline);

        PropertyBindingSupport.build()
            .withCamelContext(getCamelContext())
            .withProperties(configuration.getTransportOptions())
            .withRemoveParameters(false)
            .withTarget(consumer)
            .bind();

        configureConsumer(consumer);

        return consumer;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public Knative.Type getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public void setConfiguration(KnativeConfiguration configuration) {
        this.configuration = configuration;
    }

    public KnativeConfiguration getConfiguration() {
        return configuration;
    }

    KnativeEnvironment.KnativeServiceDefinition lookupServiceDefinition(Knative.EndpointKind endpointKind) {
        String serviceName = configuration.getServiceName();

        //
        // look-up service definition by service name first then if not found try to look it up by using
        // "default" as a service name. For channels and endpoints, the service name can be derived from
        // the endpoint uri but for events it is not possible so default should always be there for events
        // unless the service name is define as an endpoint option.
        //
        Optional<KnativeEnvironment.KnativeServiceDefinition> service = lookupServiceDefinition(serviceName, endpointKind);
        if (!service.isPresent()) {
            service = lookupServiceDefinition("default", endpointKind);
        }
        if (!service.isPresent()) {
            throw new IllegalArgumentException(String.format("Unable to find a service definition for %s/%s/%s", type, serviceName, endpointKind));
        }

        Map<String, String> metadata = new HashMap<>();
        metadata.putAll(service.get().getMetadata());

        for (Map.Entry<String, Object> entry: configuration.getFilters().entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue();

            if (val instanceof String) {
                if (!key.startsWith(Knative.KNATIVE_FILTER_PREFIX)) {
                    key = Knative.KNATIVE_FILTER_PREFIX + key;
                }

                metadata.put(key, (String)val);
            }
        }

        if (service.get().getType() == Knative.Type.event) {
            metadata.put(Knative.KNATIVE_EVENT_TYPE, serviceName);
            metadata.put(Knative.KNATIVE_FILTER_PREFIX + cloudEvent.cloudEvent().attributes().type(), serviceName);
        }

        return new KnativeEnvironment.KnativeServiceDefinition(
            service.get().getType(),
            service.get().getName(),
            service.get().getHost(),
            service.get().getPort(),
            metadata
        );
    }

    Optional<KnativeEnvironment.KnativeServiceDefinition> lookupServiceDefinition(String name, Knative.EndpointKind endpointKind) {
        return this.configuration.getEnvironment()
            .lookup(this.type, name)
            .filter(s -> {
                final String type = s.getMetadata().get(Knative.CAMEL_ENDPOINT_KIND);
                final String apiv = s.getMetadata().get(Knative.KNATIVE_API_VERSION);
                final String kind = s.getMetadata().get(Knative.KNATIVE_KIND);

                if (!Objects.equals(endpointKind.name(), type)) {
                    return false;
                }
                if (configuration.getApiVersion() != null && !Objects.equals(apiv, configuration.getApiVersion())) {
                    return false;
                }
                if (configuration.getKind() != null && !Objects.equals(kind, configuration.getKind())) {
                    return false;
                }

                return true;
            })
            .findFirst();
    }
}

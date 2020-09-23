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
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.component.knative.ce.CloudEventProcessor;
import org.apache.camel.component.knative.ce.CloudEventProcessors;
import org.apache.camel.component.knative.spi.CloudEvent;
import org.apache.camel.component.knative.spi.Knative;
import org.apache.camel.component.knative.spi.KnativeEnvironment;
import org.apache.camel.component.knative.spi.KnativeTransportConfiguration;
import org.apache.camel.processor.Pipeline;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;
import org.apache.camel.support.DefaultEndpoint;
import org.apache.camel.support.PropertyBindingSupport;
import org.apache.camel.util.ObjectHelper;

/**
 * This component allows to interact with Knative.
 */
@UriEndpoint(
    firstVersion = "3.0.0",
    scheme = "knative",
    syntax = "knative:type/name",
    title = "Knative",
    label = "cloud,eventing")
public class KnativeEndpoint extends DefaultEndpoint {
    @UriPath(description = "The Knative resource type")
    private final Knative.Type type;
    @UriPath(description = "The name that identifies the Knative resource")
    private final String name;
    private final CloudEventProcessor cloudEvent;
    @UriParam
    private KnativeConfiguration configuration;

    public KnativeEndpoint(String uri, KnativeComponent component, Knative.Type type, String name, KnativeConfiguration configuration) {
        super(uri, component);

        this.type = type;
        this.name = name;
        this.configuration = configuration;
        this.cloudEvent = CloudEventProcessors.fromSpecVersion(configuration.getCloudEventsSpecVersion());
    }

    @Override
    public KnativeComponent getComponent() {
        return (KnativeComponent) super.getComponent();
    }

    @Override
    public Producer createProducer() throws Exception {
        final KnativeEnvironment.KnativeResource service = lookupServiceDefinition(Knative.EndpointKind.sink);
        final Processor ceProcessor = cloudEvent.producer(this, service);
        final Producer producer = getComponent().getTransport().createProducer(this, createTransportConfiguration(service), service);

        PropertyBindingSupport.build()
            .withCamelContext(getCamelContext())
            .withProperties(configuration.getTransportOptions())
            .withRemoveParameters(false)
            .withTarget(producer)
            .bind();

        return new KnativeProducer(this, ceProcessor, e -> e.getMessage().removeHeader("Host"), producer);
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        final KnativeEnvironment.KnativeResource service = lookupServiceDefinition(Knative.EndpointKind.source);
        final Processor ceProcessor = cloudEvent.consumer(this, service);
        final Processor replyProcessor = configuration.isReplyWithCloudEvent() ? cloudEvent.producer(this, service) : null;
        final Processor pipeline = Pipeline.newInstance(getCamelContext(), ceProcessor, processor, replyProcessor);
        final Consumer consumer = getComponent().getTransport().createConsumer(this, createTransportConfiguration(service), service, pipeline);

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

    public KnativeConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(KnativeConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    protected void doInit() throws Exception {
        if (ObjectHelper.isEmpty(this.configuration .getServiceName())) {
            this.configuration .setServiceName(this.name);
        }
    }

    KnativeEnvironment.KnativeResource lookupServiceDefinition(Knative.EndpointKind endpointKind) {
        String serviceName = configuration.getServiceName();

        //
        // look-up service definition by service name first then if not found try to look it up by using
        // "default" as a service name. For channels and endpoints, the service name can be derived from
        // the endpoint uri but for events it is not possible so default should always be there for events
        // unless the service name is define as an endpoint option.
        //
        KnativeEnvironment.KnativeResource service = lookupServiceDefinition(serviceName, endpointKind)
            .or(() -> lookupServiceDefinition("default", endpointKind))
            .orElseThrow(() -> new IllegalArgumentException(String.format("Unable to find a service definition for %s/%s/%s", type, endpointKind, serviceName)));

        final Map<String, String> metadata = new HashMap<>(service.getMetadata());

        for (Map.Entry<String, Object> entry : configuration.getFilters().entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue();

            if (val instanceof String) {
                if (!key.startsWith(Knative.KNATIVE_FILTER_PREFIX)) {
                    key = Knative.KNATIVE_FILTER_PREFIX + key;
                }

                metadata.put(key, (String) val);
            }
        }

        for (Map.Entry<String, Object> entry : configuration.getCeOverride().entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue();

            if (val instanceof String) {
                if (!key.startsWith(Knative.KNATIVE_CE_OVERRIDE_PREFIX)) {
                    key = Knative.KNATIVE_CE_OVERRIDE_PREFIX + key;
                }

                metadata.put(key, (String) val);
            }
        }

        if (service.getType() == Knative.Type.event) {
            metadata.put(Knative.KNATIVE_EVENT_TYPE, serviceName);
            metadata.put(Knative.KNATIVE_FILTER_PREFIX + cloudEvent.cloudEvent().mandatoryAttribute(CloudEvent.CAMEL_CLOUD_EVENT_TYPE).http(), serviceName);
        }

        return new KnativeEnvironment.KnativeResource(
            service.getType(),
            service.getName(),
            service.getUrl(),
            metadata
        );
    }

    Optional<KnativeEnvironment.KnativeResource> lookupServiceDefinition(String name, Knative.EndpointKind endpointKind) {
        return servicesDefinitions()
            .filter(definition -> definition.matches(this.type, name))
            .filter(serviceFilter(endpointKind))
            .findFirst();
    }

    private KnativeTransportConfiguration createTransportConfiguration(KnativeEnvironment.KnativeResource definition) {
        return new KnativeTransportConfiguration(
            this.cloudEvent.cloudEvent(),
            !this.configuration.isReplyWithCloudEvent(),
            ObjectHelper.supplyIfEmpty(
                this.configuration.getReply(),
                () -> definition.getOptionalMetadata(Knative.KNATIVE_REPLY).map(Boolean::parseBoolean).orElse(true)
            )
        );
    }

    private Stream<KnativeEnvironment.KnativeResource> servicesDefinitions() {
        return Stream.concat(
            getCamelContext().getRegistry().findByType(KnativeEnvironment.KnativeResource.class).stream(),
            this.configuration.getEnvironment().stream()
        );
    }

    private Predicate<KnativeEnvironment.KnativeResource> serviceFilter(Knative.EndpointKind endpointKind) {
        return s -> {
            final String type = s.getMetadata(Knative.CAMEL_ENDPOINT_KIND);
            if (!Objects.equals(endpointKind.name(), type)) {
                return false;
            }

            final String apiv = s.getMetadata(Knative.KNATIVE_API_VERSION);
            if (configuration.getApiVersion() != null && !Objects.equals(apiv, configuration.getApiVersion())) {
                return false;
            }

            final String kind = s.getMetadata(Knative.KNATIVE_KIND);
            if (configuration.getKind() != null && !Objects.equals(kind, configuration.getKind())) {
                return false;
            }

            return true;
        };
    }
}

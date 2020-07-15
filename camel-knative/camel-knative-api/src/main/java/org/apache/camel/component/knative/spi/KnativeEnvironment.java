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

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.camel.CamelContext;
import org.apache.camel.impl.cloud.DefaultServiceDefinition;
import org.apache.camel.support.ResourceHelper;

/*
 * Assuming it is loaded from a json for now
 */
public class KnativeEnvironment {
    private final List<KnativeServiceDefinition> services;

    @JsonCreator
    public KnativeEnvironment(
        @JsonProperty(value = "services", required = true) List<KnativeServiceDefinition> services) {

        this.services = new ArrayList<>(services);
    }

    public Stream<KnativeServiceDefinition> stream() {
        return services.stream();
    }

    public Stream<KnativeServiceDefinition> lookup(Knative.Type type, String name) {
        return stream().filter(definition -> definition.matches(type, name));
    }

    // ************************
    //
    // Helpers
    //
    // ************************

    public static KnativeEnvironment mandatoryLoadFromSerializedString(CamelContext context, String configuration) throws Exception {
        try (Reader reader = new StringReader(configuration)) {
            return Knative.MAPPER.readValue(reader, KnativeEnvironment.class);
        }
    }

    public static KnativeEnvironment mandatoryLoadFromResource(CamelContext context, String path) throws Exception {
        try (InputStream is = ResourceHelper.resolveMandatoryResourceAsInputStream(context, path)) {
            //
            // read the knative environment from a file formatted as json, i.e. :
            //
            // {
            //     "services": [
            //         {
            //              "type": "channel|endpoint|event",
            //              "name": "",
            //              "host": "",
            //              "port": "",
            //              "metadata": {
            //                  "service.path": "",
            //                  "filter.header": "value",
            //                  "knative.event.type": "",
            //                  "knative.kind": "",
            //                  "knative.apiVersion": "",
            //                  "camel.endpoint.kind": "source|sink",
            //                  "ce.override.ce-type": "something",
            //              }
            //         },
            //     ]
            // }
            //
            //
            return Knative.MAPPER.readValue(is, KnativeEnvironment.class);
        }
    }

    public static KnativeServiceDefinition endpoint(Knative.EndpointKind endpointKind, String name, String host, int port) {
        return serviceBuilder(Knative.Type.endpoint, name)
            .withHost(host)
            .withPort(port)
            .withMeta(Knative.CAMEL_ENDPOINT_KIND, endpointKind)
            .build();
    }

    public static KnativeServiceDefinition endpoint(Knative.EndpointKind endpointKind, String name, String host, int port, Map<String, String> metadata) {
        return serviceBuilder(Knative.Type.endpoint, name)
            .withHost(host)
            .withPort(port)
            .withMeta(metadata)
            .withMeta(Knative.CAMEL_ENDPOINT_KIND, endpointKind)
            .build();
    }

    public static KnativeServiceDefinition sourceEndpoint(String name, Map<String, String> metadata) {
        return serviceBuilder(Knative.Type.endpoint, name)
            .withMeta(metadata)
            .withMeta(Knative.CAMEL_ENDPOINT_KIND, Knative.EndpointKind.source.name())
            .build();
    }

    public static KnativeServiceDefinition channel(Knative.EndpointKind endpointKind, String name, String host, int port) {
        return serviceBuilder(Knative.Type.channel, name)
            .withHost(host)
            .withPort(port)
            .withMeta(Knative.CAMEL_ENDPOINT_KIND, endpointKind)
            .build();
    }

    public static KnativeServiceDefinition channel(Knative.EndpointKind endpointKind, String name, String host, int port, Map<String, String> metadata) {
        return serviceBuilder(Knative.Type.channel, name)
            .withHost(host)
            .withPort(port)
            .withMeta(metadata)
            .withMeta(Knative.CAMEL_ENDPOINT_KIND, endpointKind)
            .build();
    }

    public static KnativeServiceDefinition sourceChannel(String name, Map<String, String> metadata) {
        return serviceBuilder(Knative.Type.channel, name)
            .withMeta(metadata)
            .withMeta(Knative.CAMEL_ENDPOINT_KIND, Knative.EndpointKind.source)
            .build();
    }

    public static KnativeServiceDefinition event(Knative.EndpointKind endpointKind, String name, String host, int port) {
        return serviceBuilder(Knative.Type.event, name)
            .withHost(host)
            .withPort(port)
            .withMeta(Knative.CAMEL_ENDPOINT_KIND, endpointKind)
            .build();
    }

    public static KnativeServiceDefinition sourceEvent(String name) {
        return serviceBuilder(Knative.Type.event, name)
            .withMeta(Knative.CAMEL_ENDPOINT_KIND, Knative.EndpointKind.source)
            .build();
    }

    public static KnativeServiceDefinition sourceEvent(String name, Map<String, String> metadata) {
        return serviceBuilder(Knative.Type.event, name)
            .withMeta(metadata)
            .withMeta(Knative.CAMEL_ENDPOINT_KIND, Knative.EndpointKind.source)
            .build();
    }

    public static KnativeServiceDefinition event(Knative.EndpointKind endpointKind, String name, String host, int port, Map<String, String> metadata) {
        return serviceBuilder(Knative.Type.event, name)
            .withHost(host)
            .withPort(port)
            .withMeta(metadata)
            .withMeta(Knative.CAMEL_ENDPOINT_KIND, endpointKind)
            .build();
    }

    public static KnativeEnvironment on(KnativeServiceDefinition... definitions) {
        return new KnativeEnvironment(Arrays.asList(definitions));
    }

    public static KnativeServiceBuilder serviceBuilder(Knative.Type type, String name) {
        return new KnativeServiceBuilder(type, name);
    }

    // ************************
    //
    // Types
    //
    // ************************


    public static final class KnativeServiceBuilder {
        private final Knative.Type type;
        private final String name;
        private String host;
        private Integer port;
        private Map<String, String> metadata;

        public KnativeServiceBuilder(Knative.Type type, String name) {
            this.type = type;
            this.name = name;
        }

        public KnativeServiceBuilder withHost(String host) {
            this.host = host;
            return this;
        }

        public KnativeServiceBuilder withPort(Integer port) {
            this.port = port;
            return this;
        }

        public KnativeServiceBuilder withMeta(Map<String, String> metadata) {
            if (metadata == null) {
                return this;
            }
            if (this.metadata == null) {
                this.metadata = new HashMap<>();
            }
            this.metadata.putAll(metadata);
            return this;
        }

        public KnativeServiceBuilder withMeta(String key, String value) {
            if (key == null || value == null) {
                return this;
            }
            if (this.metadata == null) {
                this.metadata = new HashMap<>();
            }
            this.metadata.put(key, value);
            return this;
        }

        public KnativeServiceBuilder withMeta(String key, Enum<?> e) {
            if (key == null || e == null) {
                return this;
            }
            if (this.metadata == null) {
                this.metadata = new HashMap<>();
            }
            this.metadata.put(key, e.name());
            return this;
        }

        public KnativeServiceDefinition build() {
            return new KnativeServiceDefinition(type, name, host, port, metadata);
        }
    }

    public static final class KnativeServiceDefinition extends DefaultServiceDefinition {
        @JsonCreator
        public KnativeServiceDefinition(
            @JsonProperty(value = "type", required = true) Knative.Type type,
            @JsonProperty(value = "name", required = true) String name,
            @JsonProperty(value = "host", required = false) String host,
            @JsonProperty(value = "port", required = false) Integer port,
            @JsonProperty(value = "metadata", required = false) Map<String, String> metadata) {

            super(
                name,
                host,
                port == null ? -1 : port,
                KnativeSupport.mergeMaps(
                    metadata,
                    Map.of(
                        Knative.KNATIVE_TYPE, type.name())
                )
            );
        }

        @Override
        public String getHost() {
            String urlAsString = getUrl();
            if (urlAsString != null) {
                try {
                    return new URL(urlAsString).getHost();
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }

            return super.getHost();
        }

        @Override
        public int getPort() {
            String urlAsString = getUrl();
            if (urlAsString != null) {
                try {
                    return new URL(urlAsString).getPort();
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }

            return super.getPort();
        }

        public Knative.Type getType() {
            return Knative.Type.valueOf(getMetadata().get(Knative.KNATIVE_TYPE));
        }

        public String getPath() {
            String urlAsString = getUrl();
            if (urlAsString != null) {
                try {
                    return new URL(urlAsString).getPath();
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }

            return getMetadata(Knative.SERVICE_META_PATH);
        }

        public String getPathOrDefault(String path) {
            return getMetadata().getOrDefault(Knative.SERVICE_META_PATH, path);
        }

        public String getEventType() {
            return getMetadata(Knative.KNATIVE_EVENT_TYPE);
        }

        public int getPortOrDefault(int port) {
            return getPort() != -1 ? getPort() : port;
        }

        public String getUrl() {
            return getMetadata(Knative.SERVICE_META_URL);
        }

        public String getMetadata(String key) {
            return getMetadata().get(key);
        }

        public Optional<String> getOptionalMetadata(String key) {
            return Optional.ofNullable(getMetadata(key));
        }

        public boolean matches(Knative.Type type, String name) {
            return Objects.equals(type.name(), getMetadata(Knative.KNATIVE_TYPE))
                && Objects.equals(name, getName());
        }
    }
}

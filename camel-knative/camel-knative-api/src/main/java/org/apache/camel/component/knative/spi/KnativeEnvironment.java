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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.camel.CamelContext;
import org.apache.camel.impl.cloud.DefaultServiceDefinition;
import org.apache.camel.support.ResourceHelper;

import static org.apache.camel.util.CollectionHelper.mapOf;

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
        return entry(
            endpointKind,
            Knative.Type.endpoint,
            name,
            host,
            port,
            Collections.emptyMap()
        );
    }

    public static KnativeServiceDefinition endpoint(Knative.EndpointKind endpointKind, String name, String host, int port, Map<String, String> metadata) {
        return entry(
            endpointKind,
            Knative.Type.endpoint,
            name,
            host,
            port,
            metadata
        );
    }

    public static KnativeServiceDefinition sourceEndpoint(String name, Map<String, String> metadata) {
        return entry(
            Knative.EndpointKind.source,
            Knative.Type.endpoint,
            name,
            null,
            -1,
            metadata
        );
    }

    public static KnativeServiceDefinition channel(Knative.EndpointKind endpointKind, String name, String host, int port) {
        return entry(
            endpointKind,
            Knative.Type.channel,
            name,
            host,
            port,
            Collections.emptyMap()
        );
    }

    public static KnativeServiceDefinition channel(Knative.EndpointKind endpointKind, String name, String host, int port, Map<String, String> metadata) {
        return entry(
            endpointKind,
            Knative.Type.channel,
            name,
            host,
            port,
            metadata
        );
    }

    public static KnativeServiceDefinition event(Knative.EndpointKind endpointKind, String name, String host, int port) {
        return entry(
            endpointKind,
            Knative.Type.event,
            name,
            host,
            port,
            Collections.emptyMap()
        );
    }

    public static KnativeServiceDefinition sourceEvent(String name) {
        return entry(
            Knative.EndpointKind.source,
            Knative.Type.event,
            name,
            null,
            -1,
            Collections.emptyMap()
        );
    }

    public static KnativeServiceDefinition sourceEvent(String name, Map<String, String> metadata) {
        return entry(
            Knative.EndpointKind.source,
            Knative.Type.event,
            name,
            null,
            -1,
            metadata
        );
    }

    public static KnativeServiceDefinition event(Knative.EndpointKind endpointKind, String name, String host, int port, Map<String, String> metadata) {
        return entry(
            endpointKind,
            Knative.Type.event,
            name,
            host,
            port,
            metadata
        );
    }

    public static KnativeServiceDefinition entry(Knative.EndpointKind endpointKind, Knative.Type type, String name, String host, int port, Map<String, String> metadata) {
        return new KnativeEnvironment.KnativeServiceDefinition(
            type,
            name,
            host,
            port,
            KnativeSupport.mergeMaps(
                metadata,
                mapOf(
                    Knative.CAMEL_ENDPOINT_KIND, endpointKind.name()
                )
            )
        );
    }

    public static KnativeEnvironment on(KnativeServiceDefinition... definitions) {
        return new KnativeEnvironment(Arrays.asList(definitions));
    }

    // ************************
    //
    // Types
    //
    // ************************

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
                    mapOf(
                        Knative.KNATIVE_TYPE, type.name())
                )
            );
        }

        public Knative.Type getType() {
            return Knative.Type.valueOf(getMetadata().get(Knative.KNATIVE_TYPE));
        }

        public String getPath() {
            return getMetadata().get(Knative.SERVICE_META_PATH);
        }

        public String getPathOrDefault(String path) {
            return getMetadata().getOrDefault(Knative.SERVICE_META_PATH, path);
        }

        public String getEventType() {
            return getMetadata().get(Knative.KNATIVE_EVENT_TYPE);
        }

        public int getPortOrDefault(int port) {
            return getPort() != -1 ? getPort() : port;
        }

        public String getMetadata(String key) {
            return getMetadata().get(key);
        }

        public boolean matches(Knative.Type type, String name) {
            return Objects.equals(type.name(), getMetadata(Knative.KNATIVE_TYPE))
                && Objects.equals(name, getName());
        }
    }
}

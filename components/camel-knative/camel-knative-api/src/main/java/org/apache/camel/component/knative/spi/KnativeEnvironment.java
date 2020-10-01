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

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.camel.CamelContext;
import org.apache.camel.support.ResourceHelper;

/*
 * Assuming it is loaded from a json for now
 */
public class KnativeEnvironment {
    private final List<KnativeResource> resources;

    public KnativeEnvironment() {
        this.resources = new ArrayList<>();
    }

    public KnativeEnvironment(Collection<KnativeResource> resources) {
        this.resources = new ArrayList<>(resources);
    }

    @JsonAlias("services")
    @JsonProperty(value = "resources", required = true)
    public List<KnativeResource> getResources() {
        return resources;
    }

    public Stream<KnativeResource> stream() {
        return resources.stream();
    }

    public Stream<KnativeResource> lookup(Knative.Type type, String name) {
        return stream().filter(definition -> definition.matches(type, name));
    }

    // ************************
    //
    // Helpers
    //
    // ************************

    public static KnativeEnvironment mandatoryLoadFromSerializedString(CamelContext context, String configuration) throws IOException {
        try (Reader reader = new StringReader(configuration)) {
            return Knative.MAPPER.readValue(reader, KnativeEnvironment.class);
        }
    }

    public static KnativeEnvironment mandatoryLoadFromResource(CamelContext context, String path) throws IOException {
        try (InputStream is = ResourceHelper.resolveMandatoryResourceAsInputStream(context, path)) {
            //
            // read the knative environment from a file formatted as json, i.e. :
            //
            // {
            //     "services": [
            //         {
            //              "type": "channel|endpoint|event",
            //              "name": "",
            //              "url": "",
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

    public static KnativeEnvironment on(KnativeResource... definitions) {
        KnativeEnvironment env = new KnativeEnvironment();
        for (KnativeResource definition : definitions) {
            env.getResources().add(definition);
        }

        return env;
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
        private String url;
        private Map<String, String> metadata;

        public KnativeServiceBuilder(Knative.Type type, String name) {
            this.type = type;
            this.name = name;
        }

        public KnativeServiceBuilder withUrl(String url) {
            this.url = url;
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

        public KnativeResource build() {
            return new KnativeResource(type, name, url, metadata);
        }
    }

    public static final class KnativeResource {
        private final String name;
        private final String url;
        private final Map<String, String> meta;

        @JsonCreator
        public KnativeResource(
            @JsonProperty(value = "type", required = true) Knative.Type type,
            @JsonProperty(value = "name", required = true) String name,
            @JsonProperty(value = "url", required = false) String url,
            @JsonProperty(value = "metadata", required = false) Map<String, String> metadata) {

            this.name = name;
            this.url = url;
            this.meta = KnativeSupport.mergeMaps(
                metadata,
                Map.of(
                    Knative.KNATIVE_TYPE, type.name())
            );
        }

        public String getName() {
            return this.name;
        }

        public Map<String, String> getMetadata() {
            return this.meta;
        }

        public Knative.Type getType() {
            return Knative.Type.valueOf(getMetadata().get(Knative.KNATIVE_TYPE));
        }

        public String getPath() {
            return getMetadata(Knative.SERVICE_META_PATH);
        }

        public String getEventType() {
            return getMetadata(Knative.KNATIVE_EVENT_TYPE);
        }

        public String getUrl() {
            return this.url != null ? this.url : getMetadata(Knative.SERVICE_META_URL);
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

        @Override
        public String toString() {
            return "KnativeResource{" +
                "name='" + name + '\'' +
                ", url='" + url + '\'' +
                ", meta=" + meta +
                '}';
        }
    }
}

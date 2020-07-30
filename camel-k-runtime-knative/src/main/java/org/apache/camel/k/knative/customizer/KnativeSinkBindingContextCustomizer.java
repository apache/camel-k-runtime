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
package org.apache.camel.k.knative.customizer;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Optional;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.camel.CamelContext;
import org.apache.camel.component.knative.spi.Knative;
import org.apache.camel.component.knative.spi.KnativeEnvironment;
import org.apache.camel.k.ContextCustomizer;
import org.apache.camel.k.annotation.Customizer;
import org.apache.camel.util.ObjectHelper;

@Customizer("sinkbinding")
public class KnativeSinkBindingContextCustomizer implements ContextCustomizer {

    private String name;

    private Knative.Type type;

    private String kind;

    private String apiVersion;

    @Override
    public void apply(CamelContext camelContext) {
        createSyntheticDefinition(camelContext, name).ifPresent(serviceDefinition -> {
            // publish the synthetic service definition
            camelContext.getRegistry().bind(name, serviceDefinition);
        });
    }

    private Optional<KnativeEnvironment.KnativeServiceDefinition> createSyntheticDefinition(
            CamelContext camelContext,
            String sinkName) {

        final String kSinkUrl = camelContext.resolvePropertyPlaceholders("{{k.sink:}}");
        final String kCeOverride = camelContext.resolvePropertyPlaceholders("{{k.ce.overrides:}}");

        if (ObjectHelper.isNotEmpty(kSinkUrl)) {
            // create a synthetic service definition to target the K_SINK url
            var serviceBuilder = KnativeEnvironment.serviceBuilder(type, sinkName)
                    .withMeta(Knative.CAMEL_ENDPOINT_KIND, Knative.EndpointKind.sink)
                    .withMeta(Knative.SERVICE_META_URL, kSinkUrl);

            if (ObjectHelper.isNotEmpty(kind)) {
                serviceBuilder = serviceBuilder.withMeta(Knative.KNATIVE_KIND, kind);
            }

            if (ObjectHelper.isNotEmpty(apiVersion)) {
                serviceBuilder = serviceBuilder.withMeta(Knative.KNATIVE_API_VERSION, apiVersion);
            }

            if (ObjectHelper.isNotEmpty(kCeOverride)) {
                try (Reader reader = new StringReader(kCeOverride)) {
                    // assume K_CE_OVERRIDES is defined as simple key/val json
                    var overrides = Knative.MAPPER.readValue(
                            reader,
                            new TypeReference<HashMap<String, String>>() {
                            }
                    );

                    for (var entry : overrides.entrySet()) {
                        // generate proper ce-override meta-data for the service
                        // definition
                        serviceBuilder.withMeta(
                                Knative.KNATIVE_CE_OVERRIDE_PREFIX + entry.getKey(),
                                entry.getValue()
                        );
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            return Optional.of(serviceBuilder.build());
        }

        return Optional.empty();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Knative.Type getType() {
        return type;
    }

    public void setType(Knative.Type type) {
        this.type = type;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

}

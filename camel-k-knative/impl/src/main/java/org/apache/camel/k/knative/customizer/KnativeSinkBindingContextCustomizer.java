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

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.camel.CamelContext;
import org.apache.camel.component.knative.spi.Knative;
import org.apache.camel.component.knative.spi.KnativeResource;
import org.apache.camel.k.ContextCustomizer;
import org.apache.camel.k.annotation.Customizer;
import org.apache.camel.spi.Configurer;
import org.apache.camel.util.ObjectHelper;

@Configurer
@Customizer("sinkbinding")
public class KnativeSinkBindingContextCustomizer implements ContextCustomizer {
    private String name;
    private Knative.Type type;
    private String kind;
    private String apiVersion;

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

    @Override
    public void apply(CamelContext camelContext) {
        final String kSinkUrl = camelContext.resolvePropertyPlaceholders("{{k.sink:}}");
        final String kCeOverride = camelContext.resolvePropertyPlaceholders("{{k.ce.overrides:}}");

        if (ObjectHelper.isNotEmpty(kSinkUrl)) {
            // create a synthetic service definition to target the K_SINK url
            KnativeResource resource = new KnativeResource();
            resource.setEndpointKind(Knative.EndpointKind.sink);
            resource.setType(type);
            resource.setName(name);
            resource.setUrl(kSinkUrl);
            resource.setObjectApiVersion(apiVersion);
            resource.setObjectKind(kind);
            if (type == Knative.Type.event) {
                resource.setObjectName(name);
            }

            if (ObjectHelper.isNotEmpty(kCeOverride)) {
                try (Reader reader = new StringReader(kCeOverride)) {
                    // assume K_CE_OVERRIDES is defined as simple key/val json
                    Knative.MAPPER.readValue(
                        reader,
                        new TypeReference<HashMap<String, String>>() {
                        }
                    ).forEach(resource::addCeOverride);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            camelContext.getRegistry().bind(name, resource);
        }
    }
}

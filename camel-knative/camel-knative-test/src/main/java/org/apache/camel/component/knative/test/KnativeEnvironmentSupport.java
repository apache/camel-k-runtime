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
package org.apache.camel.component.knative.test;

import java.util.Map;

import org.apache.camel.component.knative.spi.Knative;
import org.apache.camel.component.knative.spi.KnativeEnvironment;

public final class KnativeEnvironmentSupport {
    private KnativeEnvironmentSupport() {
    }

    public static KnativeEnvironment.KnativeServiceDefinition endpoint(Knative.EndpointKind endpointKind, String name, String host, int port) {
        return KnativeEnvironment.serviceBuilder(Knative.Type.endpoint, name)
            .withHost(host)
            .withPort(port)
            .withMeta(Knative.CAMEL_ENDPOINT_KIND, endpointKind)
            .build();
    }

    public static KnativeEnvironment.KnativeServiceDefinition endpoint(Knative.EndpointKind endpointKind, String name, String host, int port, Map<String, String> metadata) {
        return KnativeEnvironment.serviceBuilder(Knative.Type.endpoint, name)
            .withHost(host)
            .withPort(port)
            .withMeta(metadata)
            .withMeta(Knative.CAMEL_ENDPOINT_KIND, endpointKind)
            .build();
    }

    public static KnativeEnvironment.KnativeServiceDefinition sourceEndpoint(String name, Map<String, String> metadata) {
        return KnativeEnvironment.serviceBuilder(Knative.Type.endpoint, name)
            .withMeta(metadata)
            .withMeta(Knative.CAMEL_ENDPOINT_KIND, Knative.EndpointKind.source.name())
            .build();
    }

    public static KnativeEnvironment.KnativeServiceDefinition channel(Knative.EndpointKind endpointKind, String name, String host, int port) {
        return KnativeEnvironment.serviceBuilder(Knative.Type.channel, name)
            .withHost(host)
            .withPort(port)
            .withMeta(Knative.CAMEL_ENDPOINT_KIND, endpointKind)
            .build();
    }

    public static KnativeEnvironment.KnativeServiceDefinition channel(Knative.EndpointKind endpointKind, String name, String host, int port, Map<String, String> metadata) {
        return KnativeEnvironment.serviceBuilder(Knative.Type.channel, name)
            .withHost(host)
            .withPort(port)
            .withMeta(metadata)
            .withMeta(Knative.CAMEL_ENDPOINT_KIND, endpointKind)
            .build();
    }

    public static KnativeEnvironment.KnativeServiceDefinition sourceChannel(String name, Map<String, String> metadata) {
        return KnativeEnvironment.serviceBuilder(Knative.Type.channel, name)
            .withMeta(metadata)
            .withMeta(Knative.CAMEL_ENDPOINT_KIND, Knative.EndpointKind.source)
            .build();
    }

    public static KnativeEnvironment.KnativeServiceDefinition event(Knative.EndpointKind endpointKind, String name, String host, int port) {
        return KnativeEnvironment.serviceBuilder(Knative.Type.event, name)
            .withHost(host)
            .withPort(port)
            .withMeta(Knative.CAMEL_ENDPOINT_KIND, endpointKind)
            .build();
    }

    public static KnativeEnvironment.KnativeServiceDefinition sourceEvent(String name) {
        return KnativeEnvironment.serviceBuilder(Knative.Type.event, name)
            .withMeta(Knative.CAMEL_ENDPOINT_KIND, Knative.EndpointKind.source)
            .build();
    }

    public static KnativeEnvironment.KnativeServiceDefinition sourceEvent(String name, Map<String, String> metadata) {
        return KnativeEnvironment.serviceBuilder(Knative.Type.event, name)
            .withMeta(metadata)
            .withMeta(Knative.CAMEL_ENDPOINT_KIND, Knative.EndpointKind.source)
            .build();
    }

    public static KnativeEnvironment.KnativeServiceDefinition event(Knative.EndpointKind endpointKind, String name, String host, int port, Map<String, String> metadata) {
        return KnativeEnvironment.serviceBuilder(Knative.Type.event, name)
            .withHost(host)
            .withPort(port)
            .withMeta(metadata)
            .withMeta(Knative.CAMEL_ENDPOINT_KIND, endpointKind)
            .build();
    }
}

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
package org.apache.camel.k.quarkus.it;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.apache.camel.CamelContext;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.component.knative.KnativeComponent;
import org.apache.camel.component.knative.KnativeEndpoint;
import org.apache.camel.component.knative.spi.Knative;
import org.apache.camel.component.knative.spi.KnativeEnvironment;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@RegisterForReflection(targets = { String.class })
@Path("/test")
@ApplicationScoped
public class KnativeApplication {
    @Inject
    CamelContext context;
    @Inject
    FluentProducerTemplate template;

    @SuppressWarnings("unchecked")
    @GET
    @Path("/inspect")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject inspect() {
        var endpoint = context.getEndpoint("knative:endpoint/from", KnativeEndpoint.class);
        var envMeta = endpoint.getConfiguration().getEnvironment().lookup(Knative.Type.endpoint, "from")
            .filter(entry -> Knative.EndpointKind.source.name().equals(entry.getMetadata().get(Knative.CAMEL_ENDPOINT_KIND)))
            .findFirst()
            .map(def -> Json.createObjectBuilder((Map)def.getMetadata()))
            .orElseThrow(IllegalArgumentException::new);

        return Json.createObjectBuilder()
            .add("env-meta", envMeta)
            .add("component", Json.createObjectBuilder()
                .add("producer-factory", context.getComponent("knative", KnativeComponent.class).getProducerFactory().getClass().getName())
                .add("consumer-factory", context.getComponent("knative", KnativeComponent.class).getConsumerFactory().getClass().getName())
                .add("", "")
                .build())
            .build();
    }

    @POST
    @Path("/execute")
    @Produces(MediaType.TEXT_PLAIN)
    public String execute(String payload) {
        return template.to("direct:process").withBody(payload).request(String.class);
    }

    @javax.enterprise.inject.Produces
    KnativeEnvironment environment(
        @ConfigProperty(name = "camel.knative.listening.port") int port) {

        return KnativeEnvironment.on(
            KnativeEnvironment.serviceBuilder(Knative.Type.endpoint, "process")
                .withMeta(Knative.CAMEL_ENDPOINT_KIND, Knative.EndpointKind.source)
                .withMeta(Knative.SERVICE_META_PATH, "/knative")
                .build(),
            KnativeEnvironment.serviceBuilder(Knative.Type.endpoint, "from")
                .withMeta(Knative.CAMEL_ENDPOINT_KIND, Knative.EndpointKind.source)
                .withMeta(Knative.SERVICE_META_PATH, "/knative")
                .withMeta(Knative.KNATIVE_EVENT_TYPE, "camel.k.evt")
                .build(),
            KnativeEnvironment.serviceBuilder(Knative.Type.endpoint, "process")
                .withMeta(Knative.CAMEL_ENDPOINT_KIND, Knative.EndpointKind.sink)
                .withMeta(Knative.SERVICE_META_URL, String.format("http://localhost:%d/knative", port))
                .build()
        );
    }
}

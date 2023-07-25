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

import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.bind.JsonbBuilder;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.apache.camel.CamelContext;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.component.knative.KnativeEndpoint;
import org.apache.camel.component.knative.spi.Knative;

@RegisterForReflection(targets = { String.class })
@Path("/test")
@ApplicationScoped
public class KnativeEnvFromPropertiesApplication {
    @Inject
    CamelContext context;
    @Inject
    FluentProducerTemplate template;

    @GET
    @Path("/inspect")
    @Produces(MediaType.APPLICATION_JSON)
    public String inspect() {
        return context.getEndpoint("knative:endpoint/from", KnativeEndpoint.class)
            .getConfiguration()
            .getEnvironment()
            .lookup(Knative.Type.endpoint, "from")
                .filter(entry -> Objects.equals(Knative.EndpointKind.source, entry.getEndpointKind()))
                .findFirst()
                .map(def -> JsonbBuilder.create().toJson(def))
                .orElseThrow(IllegalArgumentException::new);
    }

    @POST
    @Path("/execute")
    @Produces(MediaType.TEXT_PLAIN)
    public String execute(String payload) {
        return template.to("direct:process").withBody(payload).request(String.class);
    }
}

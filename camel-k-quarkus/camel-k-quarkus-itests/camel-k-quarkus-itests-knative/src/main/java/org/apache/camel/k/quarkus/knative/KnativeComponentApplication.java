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
package org.apache.camel.k.quarkus.knative;

import java.util.Locale;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.knative.KnativeEndpoint;
import org.apache.camel.component.knative.spi.Knative;


@Path("/test")
@ApplicationScoped
public class KnativeComponentApplication {
    @Inject
    CamelContext context;

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
            .build();
    }

    @javax.enterprise.inject.Produces
    public RouteBuilder routes() {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("knative:endpoint/from")
                    .transform().body(String.class, b -> b.toUpperCase(Locale.US));
            }
        };
    }
}

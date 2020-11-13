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
package org.apache.camel.k.quarkus.it.knative.source;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.quarkus.arc.Unremovable;
import org.apache.camel.CamelContext;
import org.apache.camel.component.knative.spi.Knative;
import org.apache.camel.component.knative.spi.KnativeEnvironment;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.SendDefinition;
import org.apache.camel.model.ToDefinition;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/test")
@ApplicationScoped
public class KnativeSourceApplication {
    @Inject
    CamelContext context;

    private final BlockingQueue<String> queue = new LinkedBlockingQueue<>();

    @POST
    @Path("/toUpper")
    @Produces(MediaType.TEXT_PLAIN)
    public void toUpper(String data) throws InterruptedException {
        queue.put(data.toUpperCase(Locale.US));
    }

    @GET
    @Path("/inspect/endpoint-uris")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonArray endpointUris() {
        List<RouteDefinition> definitions = context.adapt(ModelCamelContext.class).getRouteDefinitions();

        return Json.createArrayBuilder(
            definitions.stream()
                .map(d -> !d.getOutputs().isEmpty() ? d.getOutputs().get(d.getOutputs().size() -1 ) : null)
                .filter(Objects::nonNull)
                .filter(ToDefinition.class::isInstance)
                .map(ToDefinition.class::cast)
                .map(SendDefinition::getEndpointUri)
                .collect(Collectors.toList())
        ).build();
    }

    @POST
    @Path("/send")
    @Produces(MediaType.TEXT_PLAIN)
    public void send(String data) {
        context.createFluentProducerTemplate()
            .to("direct:start")
            .withHeader("MyHeader", data)
            .send();
    }

    @GET
    @Path("/poll")
    @Produces(MediaType.TEXT_PLAIN)
    public Response poll()  {
        String result = queue.poll();

        return result != null
            ? Response.ok(result).build()
            : Response.noContent().build();
    }

    @Unremovable
    @javax.enterprise.inject.Produces
    KnativeEnvironment environment(
        @ConfigProperty(name = "kamel.k.sink.port") int port) {

        return KnativeEnvironment.on(
            KnativeEnvironment.serviceBuilder(Knative.Type.endpoint, "sink")
                .withUrl("http://localhost:" + port + "/test/toUpper")
                .withEndpointKind(Knative.EndpointKind.sink)
                .build()
        );
    }
}


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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.apache.camel.CamelContext;
import org.apache.camel.component.knative.spi.KnativeResource;
import org.apache.camel.k.knative.customizer.KnativeSinkBindingContextCustomizer;

@Path("/test")
@ApplicationScoped
public class KnativeSinkBindingApplication {
    @Inject
    CamelContext context;

    @GET
    @Path("/customizers/sinkbinding")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject sinkbindingCustomizer() {
        var customizer = context.getRegistry().lookupByNameAndType("sinkbinding", KnativeSinkBindingContextCustomizer.class);
        if (customizer == null) {
            return Json.createObjectBuilder().build();
        }

        return Json.createObjectBuilder()
            .add("name", customizer.getName())
            .add("apiVersion", customizer.getApiVersion())
            .add("kind", customizer.getKind())
            .add("type", customizer.getType().name())
            .build();
    }

    @GET
    @Path("/resource/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject resource(@PathParam("name") String name) {
        var resource = context.getRegistry().lookupByNameAndType(name, KnativeResource.class);
        if (resource == null) {
            return Json.createObjectBuilder().build();
        }

        return Json.createObjectBuilder()
            .add("url", resource.getUrl())
            .add("name", resource.getName())
            .add("type", resource.getType().name())
            .add("apiVersion", resource.getObjectApiVersion())
            .add("kind", resource.getObjectKind())
            .build();
    }
}


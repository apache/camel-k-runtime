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

import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.apache.camel.CamelContext;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.RouteTemplateDefinition;

@RegisterForReflection(targets = { String.class })
@Path("/test")
@ApplicationScoped
public class Application {
    @Inject
    CamelContext context;
    @Inject
    FluentProducerTemplate template;

    @GET
    @Path("/inspect")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject inspect() {
        var templates = context.adapt(ModelCamelContext.class).getRouteTemplateDefinitions();
        var ids = templates.stream().map(RouteTemplateDefinition::getId).collect(Collectors.toList());

        return Json.createObjectBuilder()
            .add("templates", Json.createArrayBuilder(ids))
            .build();
    }

    @GET
    @Path("/execute/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    public String execute(@PathParam("id") String id) {
        return template.to("direct:" + id).request(String.class);
    }

    @POST
    @Path("/execute/{templateId}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String invoke(@PathParam("templateId") String templateId, String message) {
        return template.toF("kamelet:%s/test?message=%s", templateId, message).request(String.class);
    }
}

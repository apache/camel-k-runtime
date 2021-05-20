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
package org.apache.camel.k.runtime.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.CamelContext;
import org.apache.camel.Route;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.ToDefinition;
import org.apache.camel.model.rest.RestDefinition;
import org.eclipse.microprofile.config.Config;

import static org.apache.camel.model.ProcessorDefinitionHelper.filterTypeInOutputs;

@Path("/runtime")
@ApplicationScoped
public class RuntimeInspector {
    @Inject
    CamelContext camelContext;
    @Inject
    Config config;

    @GET
    @Path("/inspect")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject inspect() {
        return Json.createObjectBuilder()
            .add("routes", Json.createArrayBuilder(
                camelContext.getRoutes().stream()
                    .map(Route::getId)
                    .collect(Collectors.toList())))
            .add("route-definitions", Json.createArrayBuilder(
                camelContext.adapt(ModelCamelContext.class).getRouteDefinitions().stream()
                    .map(RouteDefinition::getId)
                    .collect(Collectors.toList())))
            .add("rest-definitions", Json.createArrayBuilder(
                camelContext.adapt(ModelCamelContext.class).getRestDefinitions().stream()
                    .map(RestDefinition::getId)
                    .collect(Collectors.toList())))
            .build();
    }

    @GET
    @Path("/property/{name}")
    @Produces(MediaType.TEXT_PLAIN)
    public String property(@PathParam("name") String name) {
        return config.getValue(name, String.class);
    }

    @GET
    @Path("/registry/beans/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public String bean(@PathParam("name") String name) {
        Object bean = camelContext.getRegistry().lookupByName(name);
        if (bean == null) {
            throw new IllegalArgumentException("Bean with name: " + name + " not found");
        }

        return JsonbBuilder.create().toJson(bean);
    }

    @GET
    @Path("/route-outputs/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonArray routeOutputs(@PathParam("name") String name) {
        RouteDefinition def = camelContext.adapt(ModelCamelContext.class).getRouteDefinition(name);
        if (def == null) {
            throw new IllegalArgumentException("RouteDefinition with name: " + name + " not found");
        }

        Collection<ToDefinition> toDefinitions = filterTypeInOutputs(def.getOutputs(), ToDefinition.class);

        List<String> endpoints = toDefinitions.stream()
                .map(td -> td.getEndpointUri())
                .collect(Collectors.toList());

        return Json.createArrayBuilder(endpoints).build();
    }
}

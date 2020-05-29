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
package org.apache.camel.k.loader.js.quarkus.deployment;

import java.nio.charset.StandardCharsets;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.CamelContext;
import org.apache.camel.Consume;
import org.apache.camel.k.Runtime;
import org.apache.camel.k.Source;
import org.apache.camel.k.SourceLoader;
import org.apache.camel.k.Sources;
import org.apache.camel.k.loader.js.JavaScriptSourceLoader;

@Path("/test")
@ApplicationScoped
public class Application {
    @Inject
    CamelContext context;

    @POST
    @Path("/load-routes/{name}")
    @Consume(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject loadRoutes(@PathParam("name") String name, String code) throws Exception {
        final Runtime runtime = Runtime.on(context);
        final Source source = Sources.fromBytes(name, "js", null, code.getBytes(StandardCharsets.UTF_8));
        final SourceLoader loader = new JavaScriptSourceLoader();
        final SourceLoader.Result result = loader.load(Runtime.on(context), source);

        result.builder().ifPresent(runtime::addRoutes);
        result.configuration().ifPresent(runtime::addConfiguration);

        loader.load(Runtime.on(context), source);

        return Json.createObjectBuilder()
            .add("components", extractComponents())
            .add("routes", extractRoutes())
            .add("endpoints", extractEndpoints())
            .build();
    }


    private JsonArrayBuilder extractComponents() {
        JsonArrayBuilder answer = Json.createArrayBuilder();
        context.getComponentNames().forEach(answer::add);

        return answer;
    }

    private JsonArrayBuilder extractRoutes() {
        JsonArrayBuilder answer = Json.createArrayBuilder();
        context.getRoutes().forEach(r -> answer.add(r.getId()));

        return answer;
    }

    private JsonArrayBuilder extractEndpoints() {
        JsonArrayBuilder answer = Json.createArrayBuilder();
        context.getEndpoints().forEach(e -> answer.add(e.getEndpointUri()));

        return answer;
    }
}

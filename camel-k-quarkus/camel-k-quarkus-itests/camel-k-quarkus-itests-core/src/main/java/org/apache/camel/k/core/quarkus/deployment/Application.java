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
package org.apache.camel.k.core.quarkus.deployment;

import java.util.ServiceLoader;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.CamelContext;
import org.apache.camel.k.Runtime;

@Path("/test")
@ApplicationScoped
public class Application {
    @Inject
    CamelContext camelContext;

    @GET
    @Path("/services")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject getServices() {
        JsonArrayBuilder builder = Json.createArrayBuilder();

        ServiceLoader.load(Runtime.Listener.class).forEach(listener -> {
            builder.add(listener.getClass().getName());
        });

        return Json.createObjectBuilder()
            .add("services", builder)
            .build();
    }

    @GET
    @Path("/application-classloader")
    @Produces(MediaType.TEXT_PLAIN)
    public String getApplicationClassloader() {
        return camelContext.getApplicationContextClassLoader().getClass().getName();
    }
}

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
package org.apache.camel.k.loader.support;

import java.nio.charset.StandardCharsets;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.k.Runtime;
import org.apache.camel.k.Source;
import org.apache.camel.k.SourceLoader;
import org.apache.camel.k.support.Sources;

public final class LoaderSupport {
    private LoaderSupport() {
    }

    public static JsonObject inspectSource(CamelContext context, String name, String loaderId, byte[] code) throws Exception {
        final SourceLoader loader = context.getRegistry().lookupByNameAndType(loaderId, SourceLoader.class);
        final Runtime runtime = Runtime.on(context);
        final Source source = Sources.fromBytes(name, loaderId, null, code);
        final RoutesBuilder builder = loader.load(Runtime.on(context), source);

        runtime.addRoutes(builder);

        return Json.createObjectBuilder()
            .add("components", extractComponents(context))
            .add("routes", extractRoutes(context))
            .add("endpoints", extractEndpoints(context))
            .build();
    }

    public static JsonObject inspectSource(CamelContext context, String name, String loaderId, String code) throws Exception {
        return inspectSource(context, name, loaderId, code.getBytes(StandardCharsets.UTF_8));
    }


    public static JsonArrayBuilder extractComponents(CamelContext context) {
        JsonArrayBuilder answer = Json.createArrayBuilder();
        context.getComponentNames().forEach(answer::add);

        return answer;
    }

    public static JsonArrayBuilder extractRoutes(CamelContext context) {
        JsonArrayBuilder answer = Json.createArrayBuilder();
        context.getRoutes().forEach(r -> answer.add(r.getId()));

        return answer;
    }

    public static JsonArrayBuilder extractEndpoints(CamelContext context) {
        JsonArrayBuilder answer = Json.createArrayBuilder();
        context.getEndpoints().forEach(e -> answer.add(e.getEndpointUri()));

        return answer;
    }
}

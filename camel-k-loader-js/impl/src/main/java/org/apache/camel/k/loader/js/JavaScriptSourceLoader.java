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
package org.apache.camel.k.loader.js;

import java.io.Reader;
import java.util.Collections;
import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.endpoint.EndpointRouteBuilder;
import org.apache.camel.k.Runtime;
import org.apache.camel.k.Source;
import org.apache.camel.k.SourceLoader;
import org.apache.camel.k.annotation.Loader;
import org.apache.camel.k.loader.js.dsl.IntegrationConfiguration;
import org.apache.camel.k.support.RouteBuilders;
import org.apache.camel.support.LifecycleStrategySupport;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import static org.graalvm.polyglot.Source.newBuilder;

@Loader("js")
public class JavaScriptSourceLoader implements SourceLoader {
    private static final String LANGUAGE_ID = "js";

    @Override
    public List<String> getSupportedLanguages() {
        return Collections.singletonList(LANGUAGE_ID);
    }

    @Override
    public RoutesBuilder load(Runtime runtime, Source source) {
        return RouteBuilders.endpoint(source, JavaScriptSourceLoader::doLoad);
    }

    private static void doLoad(Reader reader, EndpointRouteBuilder builder) {
        final Context context = Context.newBuilder("js").allowAllAccess(true).build();
        final Value bindings = context.getBindings(LANGUAGE_ID);

        // configure bindings
        bindings.putMember("__dsl", new IntegrationConfiguration(builder));

        //
        // Expose IntegrationConfiguration methods to global scope.
        //
        context.eval(
            LANGUAGE_ID,
            "Object.setPrototypeOf(globalThis, new Proxy(Object.prototype, {"
            + "    has(target, key) {"
            + "        return key in __dsl || key in target;"
            + "    },"
            + "    get(target, key, receiver) {"
            + "        return Reflect.get((key in __dsl) ? __dsl : target, key, receiver);"
            + "    }"
            + "}));");

        //
        // Run the script.
        //
        context.eval(
            newBuilder(LANGUAGE_ID, reader, "Unnamed").buildLiteral()
        );

        //
        // Close the polyglot context when the camel context stops
        //
        builder.getContext().addLifecycleStrategy(new LifecycleStrategySupport() {
            @Override
            public void onContextStopping(CamelContext camelContext) {
                context.close(true);
            }
        });
    }
}

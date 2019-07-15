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
package org.apache.camel.k.jvm.loader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.k.RoutesLoader;
import org.apache.camel.k.Source;
import org.apache.camel.k.jvm.dsl.Components;
import org.apache.camel.k.support.URIResolver;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

public class JavaScriptLoader implements RoutesLoader {
    @Override
    public List<String> getSupportedLanguages() {
        return Collections.singletonList("js");
    }

    @Override
    public RouteBuilder load(CamelContext camelContext, Source source) throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                final CamelContext context = getContext();

                try (Context ctx = createPolyglotContext(); InputStream is = URIResolver.resolve(context, source)) {
                    Value bindings = ctx.getBindings("js");
                    bindings.putMember("builder", this);
                    bindings.putMember("context", context);
                    bindings.putMember("components", new Components(context));
                    bindings.putMember("registry", context.getRegistry());

                    bindings.putMember("from", (ProxyExecutable) args -> from(args[0].asString()));
                    bindings.putMember("rest", (ProxyExecutable) args -> rest());
                    bindings.putMember("restConfiguration", (ProxyExecutable) args -> restConfiguration());

                    ctx.eval(
                        org.graalvm.polyglot.Source.newBuilder(
                            "js",
                            new InputStreamReader(is), source.getName()
                        ).build()
                    );
                }
            }
        };
    }

    private static Context createPolyglotContext() {
        return Context.newBuilder("js").allowAllAccess(true).build();
    }
}

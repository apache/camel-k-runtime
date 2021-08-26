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
package org.apache.camel.k.quarkus.cron.deployment;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.CamelContext;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dsl.yaml.YamlRoutesBuilderLoader;
import org.apache.camel.k.Runtime;
import org.apache.camel.k.cron.CronSourceLoaderInterceptor;
import org.apache.camel.k.support.Constants;
import org.apache.camel.k.support.DelegatingRuntime;
import org.apache.camel.support.ResourceHelper;

@Path("/test")
@ApplicationScoped
public class Application {
    @Inject
    CamelContext context;
    @Inject
    Runtime runtime;

    private final AtomicBoolean stopped = new AtomicBoolean();

    @GET
    @Path("/find-cron-interceptor")
    @Produces(MediaType.TEXT_PLAIN)
    public String findCronInterceptor() {
        return context.adapt(ExtendedCamelContext.class)
            .getFactoryFinder(Constants.SOURCE_LOADER_INTERCEPTOR_RESOURCE_PATH)
            .findClass("cron")
            .map(Class::getName)
            .orElse("");
    }

    @GET
    @Path("/load")
    @Produces(MediaType.TEXT_PLAIN)
    public String load() throws Exception {
        final String code = ""
            + "\n- from:"
            + "\n    uri: \"timer:tick?period=1&delay=60000\""
            + "\n    steps:"
            + "\n      - log: \"${body}\"";

        final YamlRoutesBuilderLoader loader = new YamlRoutesBuilderLoader();
        loader.setCamelContext(context);
        loader.build();

        final Runtime rt = new DelegatingRuntime(runtime) {
            @Override
            public void stop() throws Exception {
                stopped.set(true);
            }
        };

        final CronSourceLoaderInterceptor interceptor = new CronSourceLoaderInterceptor();
        interceptor.setRuntime(rt);
        interceptor.setOverridableComponents("timer");

        final RouteBuilder builder = (RouteBuilder)loader.loadRoutesBuilder(
            ResourceHelper.fromBytes(
                "my-cron.yaml",
                code.getBytes(StandardCharsets.UTF_8)
            )
        );

        builder.addLifecycleInterceptor(interceptor);

        try {
            context.addRoutes(builder);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return "" + context.getRoutesSize();
    }

    @GET
    @Path("/stopped")
    @Produces(MediaType.TEXT_PLAIN)
    public String stopped()  {
        return "" + stopped.get();
    }
}

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
package org.apache.camel.k.quarkus.it.webhook;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.quarkus.arc.Unremovable;
import org.apache.camel.CamelContext;
import org.apache.camel.dsl.yaml.YamlRoutesBuilderLoader;
import org.apache.camel.k.quarkus.it.webhook.support.DummyWebhookComponent;
import org.apache.camel.k.webhook.WebhookAction;
import org.apache.camel.quarkus.core.CamelRuntime;
import org.apache.camel.support.ResourceHelper;

@Path("/test")
@ApplicationScoped
public class Application {
    @Inject
    CamelContext context;
    @Inject
    CamelRuntime runtime;

    private Map<WebhookAction, AtomicInteger> registerCounters;

    @PostConstruct
    void setUp() {
        registerCounters = new ConcurrentHashMap<>();

        for (WebhookAction a : WebhookAction.values()) {
            registerCounters.put(a, new AtomicInteger());
        }
    }

    @POST
    @Path("/load")
    public Response load(String code) {
        try (YamlRoutesBuilderLoader loader = new YamlRoutesBuilderLoader()) {
            runtime.getCamelContext().addRoutes(
                loader.loadRoutesBuilder(ResourceHelper.fromBytes("my-webhook.yaml", code.getBytes(StandardCharsets.UTF_8)))
            );

            return Response.status(Response.Status.OK).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("/counter/{action}")
    @Produces(MediaType.TEXT_PLAIN)
    public String counter(@PathParam("action") WebhookAction action) {
        AtomicInteger result = registerCounters.get(action);
        return result != null ? Integer.toString(result.get()) : "";
    }

    @Unremovable
    @Named("dummy")
    @javax.enterprise.inject.Produces
    DummyWebhookComponent dummy() {
        return new DummyWebhookComponent(
            () -> registerCounters.get(WebhookAction.REGISTER).incrementAndGet(),
            () -> registerCounters.get(WebhookAction.UNREGISTER).incrementAndGet());
    }

    @Unremovable
    @Named("failing")
    @javax.enterprise.inject.Produces
    DummyWebhookComponent failing() {
        return new DummyWebhookComponent(
            () -> {
                throw new RuntimeException("dummy error");
            },
            () -> {
                throw new RuntimeException("dummy error");
            });
    }

    @Unremovable
    @Named("doNothing")
    @javax.enterprise.inject.Produces
    DummyWebhookComponent doNothing() {
        return new DummyWebhookComponent(
            () -> {
            },
            () -> {
            });
    }
}

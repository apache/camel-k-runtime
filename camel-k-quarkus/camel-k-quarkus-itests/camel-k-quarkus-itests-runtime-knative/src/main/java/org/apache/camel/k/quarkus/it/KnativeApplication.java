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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.component.knative.spi.Knative;
import org.apache.camel.component.knative.spi.KnativeEnvironment;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@RegisterForReflection(targets = { String.class })
@Path("/test")
@ApplicationScoped
public class KnativeApplication {
    @Inject
    FluentProducerTemplate template;

    @POST
    @Path("/execute")
    @Produces(MediaType.TEXT_PLAIN)
    public String execute(String payload) {
        return template.to("direct:process").withBody(payload).request(String.class);
    }

    @javax.enterprise.inject.Produces
    KnativeEnvironment environment(
        @ConfigProperty(name = "camel.knative.listening.port") int port) {

        return KnativeEnvironment.on(
            KnativeEnvironment.serviceBuilder(Knative.Type.endpoint, "process")
                .withMeta(Knative.CAMEL_ENDPOINT_KIND, Knative.EndpointKind.source)
                .withMeta(Knative.SERVICE_META_PATH, "/knative")
                .build(),
            KnativeEnvironment.serviceBuilder(Knative.Type.endpoint, "process")
                .withMeta(Knative.CAMEL_ENDPOINT_KIND, Knative.EndpointKind.sink)
                .withMeta(Knative.SERVICE_META_URL, String.format("http://localhost:%d/knative", port))
                .build()
        );
    }
}

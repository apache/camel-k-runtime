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
import javax.enterprise.inject.Produces;

import io.quarkus.arc.Unremovable;
import org.apache.camel.component.knative.spi.Knative;
import org.apache.camel.component.knative.spi.KnativeEnvironment;

@ApplicationScoped
public class Application {
    @Unremovable
    @Produces
    KnativeEnvironment knativeEnvironment() {
        return KnativeEnvironment.on(
            KnativeEnvironment.serviceBuilder(Knative.Type.endpoint, "sink")
                .withUrl("http://localhost:8080")
                .withMeta(Knative.CAMEL_ENDPOINT_KIND, Knative.EndpointKind.sink)
                .build()
        );
    }
}

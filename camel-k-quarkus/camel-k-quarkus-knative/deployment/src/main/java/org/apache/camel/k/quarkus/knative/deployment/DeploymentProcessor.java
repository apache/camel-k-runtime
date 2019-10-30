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
package org.apache.camel.k.quarkus.knative.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.vertx.deployment.VertxBuildItem;
import org.apache.camel.component.knative.KnativeComponent;
import org.apache.camel.component.knative.spi.KnativeEnvironment;
import org.apache.camel.k.quarkus.knative.KnativeRecorder;
import org.apache.camel.quarkus.core.CamelServiceFilter;
import org.apache.camel.quarkus.core.deployment.CamelRuntimeBeanBuildItem;
import org.apache.camel.quarkus.core.deployment.CamelServiceFilterBuildItem;

public class DeploymentProcessor {
    @BuildStep
    void servicesFilters(BuildProducer<CamelServiceFilterBuildItem> serviceFilter) {
        serviceFilter.produce(
            new CamelServiceFilterBuildItem(CamelServiceFilter.forComponent("knative"))
        );
        serviceFilter.produce(
            new CamelServiceFilterBuildItem(CamelServiceFilter.forPathEndingWith(CamelServiceFilter.CAMEL_SERVICE_BASE_PATH + "/knative/transport/http"))
        );
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    CamelRuntimeBeanBuildItem knativeComponent(KnativeRecorder recorder, VertxBuildItem vertx) {
        return new CamelRuntimeBeanBuildItem(
            "knative",
            KnativeComponent.class,
            recorder.createKnativeComponent(vertx.getVertx())
        );
    }

    @BuildStep
    void registerReflectiveClasses(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, KnativeEnvironment.class));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, KnativeEnvironment.KnativeServiceDefinition.class));
    }
}

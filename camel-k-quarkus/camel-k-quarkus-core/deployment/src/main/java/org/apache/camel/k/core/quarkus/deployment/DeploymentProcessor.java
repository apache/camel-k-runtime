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

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.substrate.ServiceProviderBuildItem;
import org.jboss.jandex.IndexView;

import static org.apache.camel.k.core.quarkus.deployment.DeploymentSupport.getAllKnownImplementors;

public class DeploymentProcessor {
    @BuildStep
    void registerServices(
            BuildProducer<ServiceProviderBuildItem> serviceProvider,
            CombinedIndexBuildItem combinedIndexBuildItem) {

        final IndexView view = combinedIndexBuildItem.getIndex();
        final String serviceType = "org.apache.camel.k.Runtime$Listener";

        getAllKnownImplementors(view, serviceType).forEach(i-> {
            serviceProvider.produce(
                new ServiceProviderBuildItem(
                    serviceType,
                    i.name().toString())
            );
        });
    }
}

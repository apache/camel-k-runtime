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
package org.apache.camel.k.quarkus.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.runtime.configuration.TemporaryConfigSourceProvider;

public class DeploymentProcessor {
    @BuildStep
    void registerReflectiveClasses(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        // TODO: this is needed as workaround for https://github.com/quarkusio/quarkus/issues/3498
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, TemporaryConfigSourceProvider.class));
    }
}

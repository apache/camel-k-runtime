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

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.apache.camel.k.Runtime;
import org.apache.camel.k.quarkus.ApplicationRecorder;
import org.apache.camel.quarkus.main.CamelMainApplication;
import org.apache.camel.quarkus.main.deployment.spi.CamelMainListenerBuildItem;

public class DeploymentProcessor {
    @BuildStep
    public ReflectiveClassBuildItem reflectiveClasses() {
        return new ReflectiveClassBuildItem(true, false, CamelMainApplication.class);
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    CamelMainListenerBuildItem registerListener(ApplicationRecorder recorder) {
        List<Runtime.Listener> listeners = new ArrayList<>();
        ServiceLoader.load(Runtime.Listener.class).forEach(listeners::add);

        return new CamelMainListenerBuildItem(recorder.createMainListener(listeners));
    }
}

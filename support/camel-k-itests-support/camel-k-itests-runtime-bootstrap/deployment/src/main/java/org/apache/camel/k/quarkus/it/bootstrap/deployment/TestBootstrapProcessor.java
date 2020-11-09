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
package org.apache.camel.k.quarkus.it.bootstrap.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.RawCommandLineArgumentsBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import org.apache.camel.k.quarkus.it.bootstrap.TestBoostrapRecorder;
import org.apache.camel.quarkus.core.CamelConfigFlags;
import org.apache.camel.quarkus.core.deployment.spi.CamelBootstrapCompletedBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelRuntimeBuildItem;

public class TestBootstrapProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem("camel.k.quarkus.it.bootstrap");
    }

    @BuildStep(onlyIf = { CamelConfigFlags.BootstrapEnabled.class })
    @Record(value = ExecutionTime.RUNTIME_INIT)
    @Produce(CamelBootstrapCompletedBuildItem.class)
    void boot(
        TestBoostrapRecorder testRecorder,
        CamelRuntimeBuildItem runtime,
        RawCommandLineArgumentsBuildItem commandLineArguments,
        ShutdownContextBuildItem shutdown,
        BuildProducer<ServiceStartBuildItem> serviceStartBuildItems) {

        testRecorder.addShutdownTask(shutdown, runtime.runtime());
        testRecorder.start(runtime.runtime(), commandLineArguments);

        serviceStartBuildItems.produce(new ServiceStartBuildItem("camel-k-itests"));
    }
}

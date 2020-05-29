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
package org.apache.camel.k.loader.xml.quarkus.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import org.apache.camel.k.loader.xml.quarkus.XmlLoaderRecorder;
import org.apache.camel.quarkus.core.deployment.spi.CamelRoutesLoaderBuildItems;

public class DeploymentProcessor {
    @BuildStep
    @Record(value = ExecutionTime.STATIC_INIT, optional = true)
    CamelRoutesLoaderBuildItems.Xml xmlLoader(XmlLoaderRecorder recorder) {
        return new CamelRoutesLoaderBuildItems.Xml(recorder.newXMLIORoutesDefinitionLoader());
    }

    @BuildStep
    @Record(value = ExecutionTime.STATIC_INIT)
    void initXmlReifiers(XmlLoaderRecorder recorder) {
        recorder.initXmlReifiers();
    }
}

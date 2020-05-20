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

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import org.apache.camel.k.Constants;
import org.apache.camel.k.Runtime;
import org.apache.camel.k.core.quarkus.RuntimeRecorder;
import org.apache.camel.quarkus.core.deployment.CamelMainListenerBuildItem;
import org.apache.camel.quarkus.core.deployment.CamelServicePatternBuildItem;
import org.apache.camel.spi.HasId;
import org.apache.camel.spi.StreamCachingStrategy;
import org.jboss.jandex.IndexView;

import static org.apache.camel.k.core.quarkus.deployment.DeploymentSupport.getAllKnownImplementors;

public class DeploymentProcessor {
    @BuildStep
    List<CamelServicePatternBuildItem> servicePatterns() {
        return List.of(
            new CamelServicePatternBuildItem(
                CamelServicePatternBuildItem.CamelServiceDestination.REGISTRY,
                true,
                Constants.SOURCE_LOADER_RESOURCE_PATH + "/*",
                Constants.CONTEXT_CUSTOMIZER_RESOURCE_PATH  + "/*"),
            new CamelServicePatternBuildItem(
                CamelServicePatternBuildItem.CamelServiceDestination.DISCOVERY,
                true,
                Constants.SOURCE_LOADER_INTERCEPTOR_RESOURCE_PATH + "/*")
        );

    }

    @BuildStep
    void registerServices(
            BuildProducer<ServiceProviderBuildItem> serviceProvider,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            CombinedIndexBuildItem combinedIndexBuildItem) {

        final IndexView view = combinedIndexBuildItem.getIndex();
        final String serviceType = "org.apache.camel.k.Runtime$Listener";

        getAllKnownImplementors(view, serviceType).forEach(i -> {
            serviceProvider.produce(
                new ServiceProviderBuildItem(
                    serviceType,
                    i.name().toString())
            );
        });
    }

    @BuildStep
    void registerStreamCachingClasses(
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            CombinedIndexBuildItem combinedIndexBuildItem) {

        final IndexView view = combinedIndexBuildItem.getIndex();

        getAllKnownImplementors(view, StreamCachingStrategy.class).forEach(i-> {
            reflectiveClass.produce(
                new ReflectiveClassBuildItem(
                    true,
                    true,
                    i.name().toString())
            );
        });
        getAllKnownImplementors(view, StreamCachingStrategy.Statistics.class).forEach(i-> {
            reflectiveClass.produce(
                new ReflectiveClassBuildItem(
                    true,
                    true,
                    i.name().toString())
            );
        });
        getAllKnownImplementors(view, StreamCachingStrategy.SpoolRule.class).forEach(i-> {
            reflectiveClass.produce(
                new ReflectiveClassBuildItem(
                    true,
                    true,
                    i.name().toString())
            );
        });

        reflectiveClass.produce(
            new ReflectiveClassBuildItem(
                true,
                true,
                StreamCachingStrategy.SpoolRule.class)
        );
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    CamelMainListenerBuildItem registerListener(RuntimeRecorder recorder) {
        List<Runtime.Listener> listeners = new ArrayList<>();
        ServiceLoader.load(Runtime.Listener.class).forEach(listener -> {
            if (listener instanceof HasId) {
                String id = ((HasId) listener).getId();
                if (!id.endsWith(".")) {
                    id = id + ".";
                }

                // TODO: this has to be done at runtime
                //PropertiesSupport.bindProperties(getCamelContext(), listener, id);
            }

            listeners.add(listener);
        });

        return new CamelMainListenerBuildItem(recorder.createMainListener(listeners));
    }
}

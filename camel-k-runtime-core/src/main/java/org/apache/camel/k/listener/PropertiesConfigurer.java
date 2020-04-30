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
package org.apache.camel.k.listener;

import org.apache.camel.Ordered;
import org.apache.camel.k.Constants;
import org.apache.camel.k.Runtime;
import org.apache.camel.k.support.KubernetesPropertiesFunction;
import org.apache.camel.k.support.PropertiesSupport;

public class PropertiesConfigurer extends AbstractPhaseListener {
    public PropertiesConfigurer() {
        super(Runtime.Phase.ConfigureProperties);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST;
    }

    @Override
    protected void accept(Runtime runtime) {
        runtime.setInitialProperties(
            PropertiesSupport.loadApplicationProperties()
        );
        runtime.setPropertiesLocations(
            PropertiesSupport.resolveUserPropertiesLocations()
        );

        //
        // Register properties functions to resolve k8s secrets or config maps like:
        //
        // {{secret:name/key}}
        // {{configmap:name/key}}
        //

        //
        // ConfigMap
        //
        String cmPath = System.getProperty(
            Constants.PROPERTY_CAMEL_K_MOUNT_PATH_CONFIGMAPS,
            System.getenv(Constants.ENV_CAMEL_K_MOUNT_PATH_CONFIGMAPS)
        );

        if (cmPath != null) {
            runtime.getCamelContext().getPropertiesComponent().addPropertiesFunction(
                new KubernetesPropertiesFunction(cmPath, "configmap")
            );
        }

        //
        // Secret
        //
        String secretPath = System.getProperty(
            Constants.PROPERTY_CAMEL_K_MOUNT_PATH_SECRETS,
            System.getenv(Constants.ENV_CAMEL_K_MOUNT_PATH_SECRETS)
        );

        if (secretPath != null) {
            runtime.getCamelContext().getPropertiesComponent().addPropertiesFunction(
                new KubernetesPropertiesFunction(secretPath, "secret")
            );
        }
    }
}

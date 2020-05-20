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
package org.apache.camel.k.master;

import java.util.Collections;

import org.apache.camel.CamelContext;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.component.kubernetes.cluster.KubernetesClusterService;
import org.apache.camel.k.ContextCustomizer;
import org.apache.camel.util.ObjectHelper;

public class MasterContextCustomizer implements ContextCustomizer {
    private String configMapName;
    private String labelKey;
    private String labelValue;

    @Override
    public void apply(CamelContext camelContext) {
        try {
            KubernetesClusterService clusterService = new KubernetesClusterService();
            if (ObjectHelper.isNotEmpty(configMapName)) {
                clusterService.setConfigMapName(this.configMapName);
            }
            if (ObjectHelper.isNotEmpty(this.labelKey) && ObjectHelper.isNotEmpty(this.labelValue)) {
                clusterService.setClusterLabels(Collections.singletonMap(this.labelKey, this.labelValue));
            }
            camelContext.addService(clusterService);
        } catch (Exception ex) {
            throw new RuntimeCamelException(ex);
        }
    }

    public String getConfigMapName() {
        return configMapName;
    }

    public void setConfigMapName(String configMapName) {
        this.configMapName = configMapName;
    }

    public String getLabelKey() {
        return labelKey;
    }

    public void setLabelKey(String labelKey) {
        this.labelKey = labelKey;
    }

    public String getLabelValue() {
        return labelValue;
    }

    public void setLabelValue(String labelValue) {
        this.labelValue = labelValue;
    }

}

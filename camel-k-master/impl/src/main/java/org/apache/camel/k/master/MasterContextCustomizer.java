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
import org.apache.camel.component.kubernetes.cluster.LeaseResourceType;
import org.apache.camel.k.ContextCustomizer;
import org.apache.camel.k.annotation.Customizer;
import org.apache.camel.support.cluster.RebalancingCamelClusterService;
import org.apache.camel.util.ObjectHelper;

@Customizer("master")
public class MasterContextCustomizer implements ContextCustomizer {
    @Deprecated
    private String configMapName;
    private String kubernetesResourceName;
    private LeaseResourceType leaseResourceType;
    private Boolean rebalancing;
    private String labelKey;
    private String labelValue;

    @Override
    public void apply(CamelContext camelContext) {
        try {
            boolean existsService = false;
            KubernetesClusterService clusterService = new KubernetesClusterService();

            if (this.rebalancing == null || this.rebalancing) {
                RebalancingCamelClusterService existingRebalancingService =  camelContext.hasService(RebalancingCamelClusterService.class);
                if (existingRebalancingService != null){
                    existsService = true;
                    clusterService = (KubernetesClusterService)existingRebalancingService.getDelegate();
                }
            } else {
                if (camelContext.hasService(KubernetesClusterService.class) != null) {
                    clusterService = camelContext.hasService(KubernetesClusterService.class);
                    existsService = true;
                }
            }

            String resourceName = this.kubernetesResourceName;
            if (ObjectHelper.isEmpty(resourceName)) {
                resourceName = this.configMapName;
            }
            if (ObjectHelper.isNotEmpty(resourceName)) {
                clusterService.setKubernetesResourceName(resourceName);
            }
            if (ObjectHelper.isNotEmpty(this.labelKey) && ObjectHelper.isNotEmpty(this.labelValue)) {
                clusterService.setClusterLabels(Collections.singletonMap(this.labelKey, this.labelValue));
            }
            if (this.leaseResourceType != null) {
                clusterService.setLeaseResourceType(this.leaseResourceType);
            }

            if (!existsService) {
                if (this.rebalancing == null || this.rebalancing) {
                    RebalancingCamelClusterService rebalancingService = new RebalancingCamelClusterService(clusterService, clusterService.getRenewDeadlineMillis());
                    camelContext.addService(rebalancingService);
                } else {
                    camelContext.addService(clusterService);
                }
            }
        } catch (Exception ex) {
            throw new RuntimeCamelException(ex);
        }
    }

    public String getKubernetesResourceName() {
        return kubernetesResourceName;
    }

    public void setKubernetesResourceName(String kubernetesResourceName) {
        this.kubernetesResourceName = kubernetesResourceName;
    }

    public LeaseResourceType getLeaseResourceType() {
        return leaseResourceType;
    }

    public void setLeaseResourceType(LeaseResourceType leaseResourceType) {
        this.leaseResourceType = leaseResourceType;
    }

    public Boolean getRebalancing() {
        return rebalancing;
    }

    public void setRebalancing(Boolean rebalancing) {
        this.rebalancing = rebalancing;
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

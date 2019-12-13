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
package org.apache.camel.component.knative;

import java.util.Map;

import org.apache.camel.RuntimeCamelException;
import org.apache.camel.component.knative.spi.CloudEvents;
import org.apache.camel.component.knative.spi.KnativeEnvironment;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriParams;

@UriParams
public class KnativeConfiguration implements Cloneable {
    @UriParam
    private KnativeEnvironment environment;
    @UriParam
    private String serviceName;
    @UriParam(defaultValue = "false")
    @Deprecated
    private boolean jsonSerializationEnabled;
    @UriParam(defaultValue = "0.3", enums = "0.1,0.2,0.3")
    private String cloudEventsSpecVersion = CloudEvents.V03.version();
    @UriParam(defaultValue = "org.apache.camel.event")
    private String cloudEventsType = "org.apache.camel.event";
    @UriParam(prefix = "transport.")
    private Map<String, Object> transportOptions;
    @UriParam(prefix = "filter.")
    private Map<String, Object> filters;
    @UriParam(prefix = "ce.override.")
    private Map<String, Object> ceOverride;
    @UriParam(label = "advanced")
    private String apiVersion;
    @UriParam(label = "advanced")
    private String kind;

    public KnativeConfiguration() {
    }

    // ************************
    //
    // Properties
    //
    // ************************

    public KnativeEnvironment getEnvironment() {
        return environment;
    }

    /**
     * The environment
     */
    public void setEnvironment(KnativeEnvironment environment) {
        this.environment = environment;
    }

    public String getServiceName() {
        return serviceName;
    }

    /**
     * The name of the service to lookup from the {@link KnativeEnvironment}.
     */
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    @Deprecated
    public boolean isJsonSerializationEnabled() {
        return jsonSerializationEnabled;
    }

    /**
     * Enables automatic serialization to JSON of the produced events.
     */
    @Deprecated
    public void setJsonSerializationEnabled(boolean jsonSerializationEnabled) {
        this.jsonSerializationEnabled = jsonSerializationEnabled;
    }

    public String getCloudEventsSpecVersion() {
        return cloudEventsSpecVersion;
    }

    /**
     * Set the version of the cloudevents spec.
     */
    public void setCloudEventsSpecVersion(String cloudEventsSpecVersion) {
        this.cloudEventsSpecVersion = cloudEventsSpecVersion;
    }

    public String getCloudEventsType() {
        return cloudEventsType;
    }

    /**
     * Set the event-type information of the produced events.
     */
    public void setCloudEventsType(String cloudEventsType) {
        this.cloudEventsType = cloudEventsType;
    }

    public Map<String, Object> getTransportOptions() {
        return transportOptions;
    }

    /**
     * Set the transport options.
     */
    public void setTransportOptions(Map<String, Object> transportOptions) {
        this.transportOptions = transportOptions;
    }

    public Map<String, Object> getFilters() {
        return filters;
    }

    /**
     * Set the filters.
     */
    public void setFilters(Map<String, Object> filters) {
        this.filters = filters;
    }

    public Map<String, Object> getCeOverride() {
        return ceOverride;
    }

    /**
     * CloudEvent headers to override
     */
    public void setCeOverride(Map<String, Object> ceOverride) {
        this.ceOverride = ceOverride;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    /**
     * The version of the k8s resource referenced by the endpoint.
     */
    public KnativeConfiguration setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
        return this;
    }

    public String getKind() {
        return kind;
    }

    /**
     * The type of the k8s resource referenced by the endpoint.
     */
    public KnativeConfiguration setKind(String kind) {
        this.kind = kind;
        return this;
    }

    // ************************
    //
    // Cloneable
    //
    // ************************

    public KnativeConfiguration copy() {
        try {
            return (KnativeConfiguration)super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeCamelException(e);
        }
    }
}

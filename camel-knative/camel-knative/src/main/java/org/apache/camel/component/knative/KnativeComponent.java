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

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.Endpoint;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.component.knative.spi.Knative;
import org.apache.camel.component.knative.spi.KnativeEnvironment;
import org.apache.camel.component.knative.spi.KnativeTransport;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.annotations.Component;
import org.apache.camel.support.DefaultComponent;
import org.apache.camel.support.service.ServiceHelper;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.PropertiesHelper;
import org.apache.camel.util.StringHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(KnativeConstants.SCHEME)
public class KnativeComponent extends DefaultComponent {
    private static final Logger LOGGER = LoggerFactory.getLogger(KnativeComponent.class);

    @Metadata
    private KnativeConfiguration configuration;

    @Metadata
    private String environmentPath;

    @Metadata(defaultValue = "http")
    private Knative.Protocol protocol = Knative.Protocol.http;

    @Metadata
    private KnativeTransport transport;

    @Metadata
    private Map<String, Object> transportOptions;

    private boolean managedTransport;

    public KnativeComponent() {
        this(null);
    }

    public KnativeComponent(CamelContext context) {
        super(context);

        this.configuration = new KnativeConfiguration();
        this.configuration.setTransportOptions(new HashMap<>());
    }

    // ************************
    //
    // Properties
    //
    // ************************

    public KnativeConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Set the configuration.
     */
    public void setConfiguration(KnativeConfiguration configuration) {
        this.configuration = ObjectHelper.notNull(configuration, "configuration");
    }

    public String getEnvironmentPath() {
        return environmentPath;
    }

    /**
     * The path ot the environment definition
     */
    public void setEnvironmentPath(String environmentPath) {
        this.environmentPath = environmentPath;
    }

    public KnativeEnvironment getEnvironment() {
        return configuration.getEnvironment();
    }

    /**
     * The environment
     */
    public void setEnvironment(KnativeEnvironment environment) {
        configuration.setEnvironment(environment);
    }

    public String getCloudEventsSpecVersion() {
        return configuration.getCloudEventsSpecVersion();
    }

    /**
     * Set the version of the cloudevents spec.
     */
    public void setCloudEventsSpecVersion(String cloudEventSpecVersion) {
        configuration.setCloudEventsSpecVersion(cloudEventSpecVersion);
    }

    public Knative.Protocol getProtocol() {
        return protocol;
    }

    /**
     * Protocol.
     */
    public KnativeComponent setProtocol(Knative.Protocol protocol) {
        this.protocol = protocol;
        return this;
    }

    public KnativeTransport getTransport() {
        return transport;
    }

    /**
     * The transport implementation.
     */
    public void setTransport(KnativeTransport transport) {
        this.transport = transport;
    }

    public Map<String, Object> getTransportOptions() {
        return configuration.getTransportOptions();
    }

    /**
     * Transport options.
     */
    public void setTransportOptions(Map<String, Object> transportOptions) {
        configuration.setTransportOptions(transportOptions);
    }

    // ************************
    //
    // Lifecycle
    //
    // ************************

    @Override
    protected void doInit() throws Exception {
        super.doInit();

        if (transport == null) {
            this.transport = getCamelContext().getRegistry().lookupByNameAndType(protocol.name(), KnativeTransport.class);

            if (this.transport == null) {
                this.transport = getCamelContext()
                    .adapt(ExtendedCamelContext.class)
                    .getFactoryFinder(Knative.KNATIVE_TRANSPORT_RESOURCE_PATH)
                    .newInstance(protocol.name(), KnativeTransport.class)
                    .orElseThrow(() -> new RuntimeException("Error creating knative transport for protocol: " + protocol.name()));

                if (transportOptions != null) {
                    setProperties(transport, new HashMap<>(transportOptions));
                }

                this.managedTransport = true;
            }
        }

        if (this.transport instanceof CamelContextAware) {
            CamelContextAware camelContextAware = (CamelContextAware)this.transport;

            if (camelContextAware.getCamelContext() == null) {
                camelContextAware.setCamelContext(getCamelContext());
            }
        }

        LOGGER.info("found knative transport: {} for protocol: {}", transport, protocol.name());
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        if (this.transport != null && managedTransport) {
            ServiceHelper.startService(this.transport);
        }
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

        if (this.transport != null && managedTransport) {
            ServiceHelper.stopService(this.transport);
        }
    }

    // ************************
    //
    //
    //
    // ************************

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        if (ObjectHelper.isEmpty(remaining)) {
            throw new IllegalArgumentException("Expecting URI in the form of: 'knative:type/name', got '" + uri + "'");
        }

        final String type = ObjectHelper.supplyIfEmpty(StringHelper.before(remaining, "/"), () -> remaining);
        final String name = StringHelper.after(remaining, "/");
        final KnativeConfiguration conf = getKnativeConfiguration();

        conf.getFilters().putAll(
            PropertiesHelper.extractProperties(parameters, "filter.", true)
        );
        conf.getTransportOptions().putAll(
            PropertiesHelper.extractProperties(parameters, "transport.", true)
        );
        conf.getCeOverride().putAll(
            PropertiesHelper.extractProperties(parameters, "ce.override.", true)
        );

        KnativeEndpoint endpoint = new KnativeEndpoint(uri, this, Knative.Type.valueOf(type), name, conf);
        setProperties(endpoint, parameters);

        return endpoint;
    }

    // ************************
    //
    // Helpers
    //
    // ************************

    private KnativeConfiguration getKnativeConfiguration() throws Exception {
        KnativeConfiguration conf = configuration.copy();

        if (conf.getTransportOptions() == null) {
            conf.setTransportOptions(new HashMap<>());
        }
        if (conf.getFilters() == null) {
            conf.setFilters(new HashMap<>());
        }
        if (conf.getCeOverride() == null) {
            conf.setCeOverride(new HashMap<>());
        }

        if (conf.getEnvironment() == null) {
            String envConfig = System.getenv(KnativeConstants.CONFIGURATION_ENV_VARIABLE);
            if (environmentPath != null) {
                conf.setEnvironment(
                    KnativeEnvironment.mandatoryLoadFromResource(getCamelContext(), this.environmentPath)
                );
            } else if (envConfig != null) {
                if (envConfig.startsWith("file:") || envConfig.startsWith("classpath:")) {
                    conf.setEnvironment(
                        KnativeEnvironment.mandatoryLoadFromResource(getCamelContext(), envConfig)
                    );
                } else {
                    conf.setEnvironment(
                        KnativeEnvironment.mandatoryLoadFromSerializedString(getCamelContext(), envConfig)
                    );
                }
            } else {
                throw new IllegalStateException("Cannot load Knative configuration from file or env variable");
            }
        }

        return conf;
    }
}

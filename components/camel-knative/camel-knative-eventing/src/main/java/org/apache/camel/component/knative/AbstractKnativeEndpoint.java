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

import java.util.concurrent.ExecutorService;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.camel.spi.UriParam;
import org.apache.camel.support.DefaultEndpoint;

/**
 * The base knative endpoint
 */
public abstract class AbstractKnativeEndpoint extends DefaultEndpoint {

    @UriParam
    private KnativeConfiguration configuration;

    private transient KubernetesClient client;

    public AbstractKnativeEndpoint(String uri, AbstractKnativeComponent component, KnativeConfiguration config) {
        super(uri, component);
        this.configuration = config;
    }

    public KnativeConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        client = KnativeHelper.getKubernetesClient(configuration);
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        if (client != null) {
            client.close();
        }
    }

    public ExecutorService createExecutor() {
        return getCamelContext().getExecutorServiceManager().newFixedThreadPool(this, "KnativeConsumer", configuration.getPoolSize());
    }

    public KubernetesClient getKubernetesClient() {
        return client;
    }

    /**
     * The kubernetes Configuration
     */
    public KnativeConfiguration getKnativeConfiguration() {
        return configuration;
    }

}

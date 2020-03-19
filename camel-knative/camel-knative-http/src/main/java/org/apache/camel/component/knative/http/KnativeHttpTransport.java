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
package org.apache.camel.component.knative.http;

import io.vertx.ext.web.client.WebClientOptions;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.Consumer;
import org.apache.camel.Endpoint;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.component.knative.spi.KnativeEnvironment;
import org.apache.camel.component.knative.spi.KnativeTransport;
import org.apache.camel.component.knative.spi.KnativeTransportConfiguration;
import org.apache.camel.k.http.PlatformHttp;
import org.apache.camel.support.service.ServiceSupport;

public class KnativeHttpTransport extends ServiceSupport implements CamelContextAware, KnativeTransport {
    private PlatformHttp platformHttp;
    private WebClientOptions vertxHttpClientOptions;
    private CamelContext camelContext;

    public KnativeHttpTransport() {
    }

    public PlatformHttp getPlatformHttp() {
        return platformHttp;
    }

    public void setPlatformHttp(PlatformHttp platformHttp) {
        this.platformHttp = platformHttp;
    }

    public WebClientOptions getClientOptions() {
        return vertxHttpClientOptions;
    }

    public void setClientOptions(WebClientOptions vertxHttpClientOptions) {
        this.vertxHttpClientOptions = vertxHttpClientOptions;
    }

    @Override
    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    @Override
    public CamelContext getCamelContext() {
        return camelContext;
    }

    // *****************************
    //
    // Lifecycle
    //
    // *****************************

    @Override
    protected void doStart() throws Exception {
        if (this.platformHttp == null) {
            this.platformHttp = PlatformHttp.lookup(camelContext);
        }
    }

    @Override
    protected void doStop() throws Exception {
    }

    // *****************************
    //
    //
    //
    // *****************************

    @Override
    public Producer createProducer(Endpoint endpoint, KnativeTransportConfiguration config, KnativeEnvironment.KnativeServiceDefinition service) {
        return new KnativeHttpProducer(this, endpoint, service, this.platformHttp, vertxHttpClientOptions);
    }

    @Override
    public Consumer createConsumer(Endpoint endpoint, KnativeTransportConfiguration config, KnativeEnvironment.KnativeServiceDefinition service, Processor processor) {
        Processor next = processor;
        if (config.isRemoveCloudEventHeadersInReply()) {
            next = KnativeHttpSupport.withoutCloudEventHeaders(processor, config.getCloudEvent());
        }
        return new KnativeHttpConsumer(this, endpoint, service, this.platformHttp, next);
    }

}

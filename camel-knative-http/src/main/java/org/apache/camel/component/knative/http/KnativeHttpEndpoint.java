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

import java.util.Map;

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.spi.HeaderFilterStrategy;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;
import org.apache.camel.support.AsyncProcessorConverterHelper;
import org.apache.camel.support.DefaultEndpoint;
import org.apache.camel.support.jsse.SSLContextParameters;

@UriEndpoint(
    firstVersion = "3.0.0",
    scheme = "knative-http",
    title = "KnativeHttp",
    syntax = "knative-http:host:port/path",
    label = "http",
    lenientProperties = true)
public class KnativeHttpEndpoint extends DefaultEndpoint {
    @UriPath
    @Metadata(required = true)
    private String host;
    @UriPath
    @Metadata(required = true)
    private int port;
    @UriPath
    private String path;

    @UriParam(label = "advanced")
    private HeaderFilterStrategy headerFilterStrategy;
    @UriParam(label = "security")
    private SSLContextParameters sslContextParameters;

    @UriParam(label = "consumer")
    private Map<String, Object> headerFilter;
    @UriParam(label = "producer", defaultValue = "true")
    private Boolean throwExceptionOnFailure = Boolean.TRUE;

    public KnativeHttpEndpoint(String uri, KnativeHttpComponent component) {
        super(uri, component);

        this.headerFilterStrategy = new KnativeHttpHeaderFilterStrategy();
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;

        if (!this.path.startsWith("/")) {
            this.path = "/" + path;
        }
    }

    public HeaderFilterStrategy getHeaderFilterStrategy() {
        return headerFilterStrategy;
    }

    public void setHeaderFilterStrategy(HeaderFilterStrategy headerFilterStrategy) {
        this.headerFilterStrategy = headerFilterStrategy;
    }

    public Map<String, Object> getHeaderFilter() {
        return headerFilter;
    }

    public void setHeaderFilter(Map<String, Object> headerFilter) {
        this.headerFilter = headerFilter;
    }

    public SSLContextParameters getSslContextParameters() {
        return sslContextParameters;
    }

    public void setSslContextParameters(SSLContextParameters sslContextParameters) {
        this.sslContextParameters = sslContextParameters;
    }

    public Boolean getThrowExceptionOnFailure() {
        return throwExceptionOnFailure;
    }

    public void setThrowExceptionOnFailure(Boolean throwExceptionOnFailure) {
        this.throwExceptionOnFailure = throwExceptionOnFailure;
    }

    public KnativeHttp.ServerKey getServerKey() {
        return new KnativeHttp.ServerKey(host, port);
    }

    @Override
    public KnativeHttpComponent getComponent() {
        return (KnativeHttpComponent)super.getComponent();
    }

    @Override
    public Producer createProducer() throws Exception {
        return new KnativeHttpProducer(this, getComponent().getVertx(), getComponent().getVertxHttpClientOptions());
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        return new KnativeHttpConsumer(this, AsyncProcessorConverterHelper.convert(processor));
    }
}

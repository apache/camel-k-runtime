/**
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
package org.apache.camel.k.servlet;

import org.apache.camel.CamelContext;
import org.apache.camel.Ordered;
import org.apache.camel.k.ContextCustomizer;

public class ServletContextCustomizer implements ContextCustomizer {
    public static final String DEFAULT_BIND_HOST = "0.0.0.0";
    public static final int DEFAULT_BIND_PORT = 8080;
    public static final String DEFAULT_PATH = "/";

    private String bindHost;
    private int bindPort;
    private String path;
    private ServletEndpoint endpoint;

    public ServletContextCustomizer() {
        this.bindHost = DEFAULT_BIND_HOST;
        this.bindPort = DEFAULT_BIND_PORT;
        this.path = DEFAULT_PATH;
    }

    public String getBindHost() {
        return bindHost;
    }

    public void setBindHost(String bindHost) {
        this.bindHost = bindHost;
    }

    public int getBindPort() {
        return bindPort;
    }

    public void setBindPort(int bindPort) {
        this.bindPort = bindPort;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public void apply(CamelContext camelContext) {
        endpoint = new ServletEndpoint(camelContext, bindHost, bindPort, path);

        try {
            camelContext.addService(endpoint, true, true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST;
    }
}

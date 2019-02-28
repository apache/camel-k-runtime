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
package org.apache.camel.k.health;

import org.apache.camel.k.Runtime;
import org.apache.camel.spi.HasId;

public class HealthConfigurer implements Runtime.Listener, HasId {
    public static final String ID = "endpoint.health";
    public static final String DEFAULT_BIND_HOST = "0.0.0.0";
    public static final int DEFAULT_BIND_PORT = 8081;
    public static final String DEFAULT_PATH = "/health";

    private HealthEndpoint endpoint;

    private String bindHost;
    private int bindPort;
    private String path;

    public HealthConfigurer() {
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
    public void accept(Runtime.Phase phase, Runtime runtime) {
        try {
            if (phase == Runtime.Phase.Starting) {
                endpoint = new HealthEndpoint(runtime.getContext(), bindHost, bindPort, path);
                endpoint.start();
            } else if (phase == Runtime.Phase.Stopping) {
                if (endpoint != null) {
                    endpoint.stop();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getId() {
        return ID;
    }
}

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

import java.util.Objects;
import java.util.regex.Pattern;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;

public final class KnativeHttp {
    public static final int DEFAULT_PORT = 8080;
    public static final String DEFAULT_PATH = "/";
    public static final Pattern ENDPOINT_PATTERN = Pattern.compile("([0-9a-zA-Z][\\w\\.-]+):(\\d+)\\/?(.*)");

    private KnativeHttp() {
    }

    public static final class ServerKey {
        private final String host;
        private final int port;

        public ServerKey(String host, int port) {
            this.host = host;
            this.port = port;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ServerKey key = (ServerKey) o;
            return getPort() == key.getPort() && getHost().equals(key.getHost());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getHost(), getPort());
        }
    }

    public interface PredicatedHandler extends Handler<HttpServerRequest> {
        boolean canHandle(HttpServerRequest event);
    }
}

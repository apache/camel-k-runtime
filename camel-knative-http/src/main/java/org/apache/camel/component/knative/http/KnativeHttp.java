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
import javax.net.ssl.SSLContext;

public final class KnativeHttp {
    private KnativeHttp() {
    }

    public static final class HostKey {
        private final String host;
        private final int port;
        private final SSLContext sslContext;

        public HostKey(String host, int port, SSLContext ssl) {
            this.host = host;
            this.port = port;
            this.sslContext = ssl;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        public SSLContext getSslContext() {
            return sslContext;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            HostKey key = (HostKey) o;
            return getPort() == key.getPort()
                && getHost().equals(key.getHost());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getHost(), getPort());
        }
    }


    /**
     * Options to configure an Undertow host.
     */
    public static final class HostOptions {

        /**
         * The number of worker threads to use in a Undertow host.
         */
        private Integer workerThreads;

        /**
         * The number of io threads to use in a Undertow host.
         */
        private Integer ioThreads;

        /**
         * The buffer size of the Undertow host.
         */
        private Integer bufferSize;

        /**
         * Set if the Undertow host should use direct buffers.
         */
        private Boolean directBuffers;

        /**
         * Set if the Undertow host should use http2 protocol.
         */
        private Boolean http2Enabled;


        public Integer getWorkerThreads() {
            return workerThreads;
        }

        public void setWorkerThreads(Integer workerThreads) {
            this.workerThreads = workerThreads;
        }

        public Integer getIoThreads() {
            return ioThreads;
        }

        public void setIoThreads(Integer ioThreads) {
            this.ioThreads = ioThreads;
        }

        public Integer getBufferSize() {
            return bufferSize;
        }

        public void setBufferSize(Integer bufferSize) {
            this.bufferSize = bufferSize;
        }

        public Boolean getDirectBuffers() {
            return directBuffers;
        }

        public void setDirectBuffers(Boolean directBuffers) {
            this.directBuffers = directBuffers;
        }

        public Boolean getHttp2Enabled() {
            return http2Enabled;
        }

        public void setHttp2Enabled(Boolean http2Enabled) {
            this.http2Enabled = http2Enabled;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("UndertowHostOptions{");
            sb.append("workerThreads=").append(workerThreads);
            sb.append(", ioThreads=").append(ioThreads);
            sb.append(", bufferSize=").append(bufferSize);
            sb.append(", directBuffers=").append(directBuffers);
            sb.append(", http2Enabled=").append(http2Enabled);
            sb.append('}');
            return sb.toString();
        }
    }
}

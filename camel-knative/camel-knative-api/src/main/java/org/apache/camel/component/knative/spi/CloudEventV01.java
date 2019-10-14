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
package org.apache.camel.component.knative.spi;

final class CloudEventV01 implements CloudEvent {
    public static final String VERSION = "0.1";
    public static final Attributes ATTRIBUTES = new Attributes() {
        @Override
        public String id() {
            return "CE-EventID";
        }

        @Override
        public String source() {
            return "CE-Source";
        }

        @Override
        public String spec() {
            return "CE-CloudEventsVersion";
        }

        @Override
        public String type() {
            return "CE-EventType";
        }

        @Override
        public String time() {
            return "CE-EventTime";
        }
    };

    @Override
    public String version() {
        return VERSION;
    }

    @Override
    public Attributes attributes() {
        return ATTRIBUTES;
    }
}

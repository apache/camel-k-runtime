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

import java.util.Properties;
import java.util.ServiceLoader;

import org.apache.camel.k.Runtime;
import org.apache.camel.k.jvm.ApplicationRuntime;

public class HealthMain {
    public static void main(String[] args) throws Exception {
        Properties p = new Properties();
        p.setProperty("endpoint.health.bindHost", "localhost");
        p.setProperty("endpoint.health.bindPort", "9988");
        p.setProperty("endpoint.health.path", "/ht");

        ApplicationRuntime runtime = new ApplicationRuntime();
        runtime.setProperties(p);
        runtime.addListeners(ServiceLoader.load(Runtime.Listener.class));
        runtime.run();
    }
}

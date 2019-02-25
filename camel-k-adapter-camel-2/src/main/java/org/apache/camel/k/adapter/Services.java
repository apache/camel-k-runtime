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
package org.apache.camel.k.adapter;

import java.util.Arrays;

import org.apache.camel.util.ServiceHelper;

public final class Services {
    private Services() {

    }

    public static void start(Object... services) throws Exception {
        ServiceHelper.startServices(services);
    }

    public static void stop(Object... services) throws Exception {
        ServiceHelper.stopServices(services);
    }

    public static void suspend(Object... services) throws Exception {
        ServiceHelper.suspendServices(Arrays.asList(services));
    }

    public static void resume(Object... services) throws Exception {
        ServiceHelper.resumeService(Arrays.asList(services));
    }

    public static void shutdown(Object... services) throws Exception {
        ServiceHelper.stopAndShutdownServices(Arrays.asList(services));
    }
}

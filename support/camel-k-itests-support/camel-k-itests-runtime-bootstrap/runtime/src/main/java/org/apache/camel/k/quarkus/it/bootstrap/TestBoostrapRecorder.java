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
package org.apache.camel.k.quarkus.it.bootstrap;

import java.util.function.Supplier;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.quarkus.core.CamelBootstrapRecorder;
import org.apache.camel.quarkus.core.CamelRuntime;
import org.jboss.logging.Logger;

@Recorder
public class TestBoostrapRecorder {
    public void addShutdownTask(ShutdownContext shutdown, RuntimeValue<CamelRuntime> runtime) {
        shutdown.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                try {
                    runtime.getValue().stop();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public void start(RuntimeValue<CamelRuntime> runtime, Supplier<String[]> arguments) {
        try {
            Logger logger = Logger.getLogger(CamelBootstrapRecorder.class);
            logger.infof("test bootstrap runtime: %s", runtime.getValue().getClass().getName());
            logger.infof("test bootstrap runtime: runtime won't be started automatically for testing purpose");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

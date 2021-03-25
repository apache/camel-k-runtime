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
package org.apache.camel.k.listener;

import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.builder.DeadLetterChannelBuilder;
import org.apache.camel.k.Runtime;
import org.apache.camel.k.support.PropertiesSupport;
import org.apache.camel.spi.Configurer;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configurer
public class GlobalErrorHandlerConfigurer extends AbstractPhaseListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalErrorHandlerConfigurer.class);

    public static final String CAMEL_K_PREFIX = "camel.k.";
    public static final String CAMEL_K_GLOBAL_ERROR_HANDLER_PREFIX = "camel.k.global-error-handler";

    private String globalErrorHandler;

    public GlobalErrorHandlerConfigurer() {
        super(Runtime.Phase.ConfigureRoutes);
    }

    public String getGlobalErrorHandler(){
        return this.globalErrorHandler;
    }

    public void setGlobalErrorHandler(String globalErrorHandler){
        this.globalErrorHandler = globalErrorHandler;
    }

    @Override
    protected void accept(Runtime runtime) {
        PropertiesSupport.bindProperties(
            runtime.getCamelContext(),
            this,
            k -> k.startsWith(CAMEL_K_GLOBAL_ERROR_HANDLER_PREFIX),
            CAMEL_K_PREFIX);
        if (ObjectHelper.isNotEmpty(this.globalErrorHandler)) {
            loadGlobalErrorHandler(runtime, this.getGlobalErrorHandler());
        }
    }

    public static void loadGlobalErrorHandler(Runtime runtime, String uri) {
        LOGGER.info("Setting a default global error handler factory to deadletter channel {}", uri);
        runtime.getCamelContext().adapt(ExtendedCamelContext.class)
                .setErrorHandlerFactory(new DeadLetterChannelBuilder(uri));
    }

}

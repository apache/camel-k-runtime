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

import org.apache.camel.k.Runtime;
import org.apache.camel.k.support.PropertiesSupport;
import org.apache.camel.spi.Configurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configurer
public class YamlConfigurer extends AbstractPhaseListener {
    public static final Logger LOGGER = LoggerFactory.getLogger(YamlConfigurer.class);
    public static final String CAMEL_K_YAML_PROPERTIES_PREFIX = "camel.k.yaml.";

    private String deserializationMode = "FLOW";

    public YamlConfigurer() {
        super(Runtime.Phase.ConfigureContext);
    }

    public String getDeserializationMode() {
        return deserializationMode;
    }

    public void setDeserializationMode(String deserializationMode) {
        this.deserializationMode = deserializationMode;
    }

    @Override
    protected void accept(Runtime runtime) {
        PropertiesSupport.bindProperties(
            runtime.getCamelContext(),
            this,
            CAMEL_K_YAML_PROPERTIES_PREFIX,
            true);

        runtime.getCamelContext().getGlobalOptions().put("CamelYamlDslDeserializationMode", getDeserializationMode());

        LOGGER.info("CamelYamlDslDeserializationMode : {}", runtime.getCamelContext().getGlobalOption("CamelYamlDslDeserializationMode"));
    }
}

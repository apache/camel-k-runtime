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
package org.apache.camel.k.loader.yaml.parser;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.camel.k.annotation.yaml.YAMLNodeDefinition;
import org.apache.camel.k.annotation.yaml.YAMLStepParser;
import org.apache.camel.k.loader.yaml.model.Step;
import org.apache.camel.k.loader.yaml.spi.ProcessorStepParser;
import org.apache.camel.k.loader.yaml.spi.StepParserSupport;
import org.apache.camel.model.CircuitBreakerDefinition;
import org.apache.camel.model.HystrixConfigurationDefinition;
import org.apache.camel.model.OnFallbackDefinition;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.Resilience4jConfigurationCommon;
import org.apache.camel.model.Resilience4jConfigurationDefinition;
import org.apache.camel.reifier.CircuitBreakerReifier;
import org.apache.camel.reifier.OnFallbackReifier;

@YAMLStepParser(id = "circuit-breaker", definition = CircuitBreakerStepParser.CBDefinition.class)
public class CircuitBreakerStepParser implements ProcessorStepParser {
    @Override
    public ProcessorDefinition<?> toProcessor(Context context) {
        CBDefinition definition = context.node(CBDefinition.class);

        ProcessorDefinition<?> processor = StepParserSupport.convertSteps(
            context,
            definition.delegate,
            definition.steps
        );

        if (definition.onFallback != null) {
            StepParserSupport.convertSteps(
                context,
                definition.onFallback,
                definition.onFallback.steps
            );

            definition.delegate.setOnFallback(definition.onFallback);
        }

        return processor;
    }

    @YAMLNodeDefinition(reifiers = CircuitBreakerReifier.class)
    public static final class CBDefinition {
        public CircuitBreakerDefinition delegate = new CircuitBreakerDefinition();

        public FBDefinition onFallback;
        @JsonProperty
        public List<Step> steps;

        public HystrixConfigurationDefinition getHystrixConfiguration() {
            return delegate.getHystrixConfiguration();
        }

        public void setHystrixConfiguration(HystrixConfigurationDefinition hystrixConfiguration) {
            delegate.setHystrixConfiguration(hystrixConfiguration);
        }

        public Resilience4jConfigurationCommon getResilience4jConfiguration() {
            return delegate.getResilience4jConfiguration();
        }

        public void setResilience4jConfiguration(Resilience4jConfigurationDefinition resilience4jConfiguration) {
            delegate.setResilience4jConfiguration(resilience4jConfiguration);
        }

        public String getConfigurationRef() {
            return delegate.getConfigurationRef();
        }

        public void setConfigurationRef(String configurationRef) {
            delegate.setConfigurationRef(configurationRef);
        }

        public FBDefinition getOnFallback() {
            return onFallback;
        }

        @JsonProperty("on-fallback")
        public void setOnFallback(FBDefinition onFallback) {
            this.onFallback = onFallback;
        }
    }

    @YAMLNodeDefinition(reifiers = OnFallbackReifier.class)
    public static final class FBDefinition extends OnFallbackDefinition {
        @JsonProperty
        public List<Step> steps;

        @Override
        public String getFallbackViaNetwork() {
            return super.getFallbackViaNetwork();
        }

        @JsonAlias("fallback-via-network")
        @Override
        public void setFallbackViaNetwork(String fallbackViaNetwork) {
            super.setFallbackViaNetwork(fallbackViaNetwork);
        }
    }
}


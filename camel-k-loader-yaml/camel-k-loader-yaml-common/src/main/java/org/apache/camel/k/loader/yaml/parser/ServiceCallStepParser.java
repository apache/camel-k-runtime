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

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.camel.k.annotation.yaml.YAMLNodeDefinition;
import org.apache.camel.k.annotation.yaml.YAMLStepParser;
import org.apache.camel.k.loader.yaml.spi.ProcessorStepParser;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.cloud.ServiceCallDefinition;
import org.apache.camel.model.cloud.ServiceCallExpressionConfiguration;
import org.apache.camel.model.cloud.ServiceCallServiceDiscoveryConfiguration;
import org.apache.camel.model.cloud.ServiceCallServiceFilterConfiguration;
import org.apache.camel.model.cloud.ServiceCallServiceLoadBalancerConfiguration;
import org.apache.camel.model.language.ExpressionDefinition;

@YAMLStepParser(id = "service-call", definitions = ServiceCallStepParser.Definition.class)
public class ServiceCallStepParser implements ProcessorStepParser {
    @Override
    public ProcessorDefinition<?> toProcessor(Context context) {
        return context.node(Definition.class).delegate;
    }

    //
    // need to use delegation instead of inheritance because of clash with
    // teh expression block.
    //
    // may need to be fixed in camel to use an ExpressionDefinition instead
    // of just Expression.
    //
    @YAMLNodeDefinition
    public static final class Definition implements HasExpression {
        private final ServiceCallDefinition delegate;

        public Definition() {
            this.delegate = new ServiceCallDefinition();
        }

        public Definition(String name) {
            this.delegate = new ServiceCallDefinition();
            this.delegate.setName(name);
        }

        //
        // Expression support
        //
        @Override
        public void setExpression(ExpressionDefinition expressionDefinition) {
            delegate.setExpression(expressionDefinition);
        }

        @Override
        public ExpressionDefinition getExpression() {
            return delegate.getExpression() != null
                ? new ExpressionDefinition(delegate.getExpression())
                : null;
        }

        //
        // Properties
        //
        public String getName() {
            return delegate.getName();
        }

        public void setName(String name) {
            delegate.setName(name);
        }

        public String getPattern() {
            return delegate.getPattern();
        }

        public void setPattern(String pattern) {
            delegate.setPattern(pattern);
        }

        public String getUri() {
            return delegate.getUri();
        }

        public void setUri(String uri) {
            delegate.setUri(uri);
        }

        public String getComponent() {
            return delegate.getComponent();
        }

        public void setComponent(String component) {
            delegate.setComponent(component);
        }

        public String getServiceDiscoveryRef() {
            return delegate.getServiceDiscoveryRef();
        }

        public void setServiceDiscoveryRef(String serviceDiscoveryRef) {
            delegate.setServiceDiscoveryRef(serviceDiscoveryRef);
        }

        public String getServiceFilterRef() {
            return delegate.getServiceFilterRef();
        }

        public void setServiceFilterRef(String serviceFilterRef) {
            delegate.setServiceFilterRef(serviceFilterRef);
        }

        public String getServiceChooserRef() {
            return delegate.getServiceChooserRef();
        }

        public void setServiceChooserRef(String serviceChooserRef) {
            delegate.setServiceChooserRef(serviceChooserRef);
        }

        public void setLoadBalancerRef(String loadBalancerRef) {
            delegate.setLoadBalancerRef(loadBalancerRef);
        }

        public String getExpressionRef() {
            return delegate.getExpressionRef();
        }

        public void setExpressionRef(String expressionRef) {
            delegate.setExpressionRef(expressionRef);
        }

        public String getConfigurationRef() {
            return delegate.getConfigurationRef();
        }

        public void setConfigurationRef(String configurationRef) {
            delegate.setConfigurationRef(configurationRef);
        }

        //
        // Service Discovery
        //
        public ServiceCallServiceDiscoveryConfiguration getServiceDiscoveryConfiguration() {
            return delegate.getServiceDiscoveryConfiguration();
        }

        public void setServiceDiscoveryConfiguration(ServiceCallServiceDiscoveryConfiguration serviceDiscoveryConfiguration) {
            delegate.setServiceDiscoveryConfiguration(serviceDiscoveryConfiguration);
        }

        //
        // Service Filter
        //
        public ServiceCallServiceFilterConfiguration getServiceFilterConfiguration() {
            return delegate.getServiceFilterConfiguration();
        }

        public void setServiceFilterConfiguration(ServiceCallServiceFilterConfiguration serviceFilterConfiguration) {
            delegate.setServiceFilterConfiguration(serviceFilterConfiguration);
        }

        //
        // Load Balancer
        //
        public ServiceCallServiceLoadBalancerConfiguration getLoadBalancerConfiguration() {
            return delegate.getLoadBalancerConfiguration();
        }

        public void setLoadBalancerConfiguration(ServiceCallServiceLoadBalancerConfiguration loadBalancerConfiguration) {
            delegate.setLoadBalancerConfiguration(loadBalancerConfiguration);
        }

        //
        // Expression
        //
        public ServiceCallExpressionDefinition getExpressionConfiguration() {
            return (ServiceCallExpressionDefinition)delegate.getExpressionConfiguration();
        }

        public void setExpressionConfiguration(ServiceCallExpressionDefinition expressionConfiguration) {
            delegate.setExpressionConfiguration(expressionConfiguration);
        }
    }

    @YAMLNodeDefinition
    public static final class ServiceCallExpressionDefinition extends ServiceCallExpressionConfiguration implements HasExpression {
        @JsonIgnore
        @Override
        public ExpressionDefinition getExpressionType() {
            return super.getExpressionType();
        }

        @JsonIgnore
        @Override
        public void setExpressionType(ExpressionDefinition expressionType) {
            super.setExpressionType(expressionType);
        }

        @Override
        public void setExpression(ExpressionDefinition expressionDefinition) {
            super.setExpressionType(expressionDefinition);
        }

        @Override
        public ExpressionDefinition getExpression() {
            return super.getExpressionType();
        }
    }
}


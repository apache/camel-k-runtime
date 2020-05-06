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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.apache.camel.Expression;
import org.apache.camel.k.annotation.yaml.YAMLNodeDefinition;
import org.apache.camel.k.annotation.yaml.YAMLStepParser;
import org.apache.camel.k.loader.yaml.model.Step;
import org.apache.camel.k.loader.yaml.spi.ProcessorStepParser;
import org.apache.camel.k.loader.yaml.spi.StepParserSupport;
import org.apache.camel.model.ExpressionSubElementDefinition;
import org.apache.camel.model.LoadBalanceDefinition;
import org.apache.camel.model.LoadBalancerDefinition;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.language.ExpressionDefinition;
import org.apache.camel.model.loadbalancer.CustomLoadBalancerDefinition;
import org.apache.camel.model.loadbalancer.FailoverLoadBalancerDefinition;
import org.apache.camel.model.loadbalancer.RandomLoadBalancerDefinition;
import org.apache.camel.model.loadbalancer.RoundRobinLoadBalancerDefinition;
import org.apache.camel.model.loadbalancer.StickyLoadBalancerDefinition;
import org.apache.camel.model.loadbalancer.TopicLoadBalancerDefinition;
import org.apache.camel.model.loadbalancer.WeightedLoadBalancerDefinition;
import org.apache.camel.reifier.LoadBalanceReifier;

@YAMLStepParser("load-balance")
public class LoadBalanceStepParser implements ProcessorStepParser {
    @Override
    public ProcessorDefinition<?> toProcessor(Context context) {
        Definition definition = context.node(Definition.class);

        return StepParserSupport.convertSteps(
            context,
            definition,
            definition.steps
        );
    }

    @YAMLNodeDefinition(reifiers = LoadBalanceReifier.class)
    public static final class Definition extends LoadBalanceDefinition {
        public List<Step> steps;

        @JsonAlias({"load-balancer-type", "type"})
        @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.WRAPPER_OBJECT
        )
        @Override
        public void setLoadBalancerType(LoadBalancerDefinition loadbalancer) {
            super.setLoadBalancerType(loadbalancer);
        }

        @Override
        public LoadBalancerDefinition getLoadBalancerType() {
            return super.getLoadBalancerType();
        }

        @JsonAlias("random")
        public void setRandom(RandomLoadBalancerDefinition definition) {
            if (getLoadBalancerType() != null) {
                throw new IllegalArgumentException("A load-balancer has already been set");
            }
            setLoadBalancerType(definition);
        }

        @JsonAlias({"customLoadBalancer", "custom"})
        public  void setCustomLoadBalancer(CustomLoadBalancerDefinition definition) {
            if (getLoadBalancerType() != null) {
                throw new IllegalArgumentException("A load-balancer has already been set");
            }
            setLoadBalancerType(definition);
        }

        @JsonAlias("failover")
        public  void setFailover(FailoverLoadBalancerDefinition definition) {
            if (getLoadBalancerType() != null) {
                throw new IllegalArgumentException("A load-balancer has already been set");
            }
            setLoadBalancerType(definition);
        }

        @JsonAlias("sticky")
        public  void setSticky(Sticky definition) {
            if (getLoadBalancerType() != null) {
                throw new IllegalArgumentException("A load-balancer has already been set");
            }
            setLoadBalancerType(definition);
        }

        @JsonAlias("topic")
        public  void setTopic(TopicLoadBalancerDefinition definition) {
            if (getLoadBalancerType() != null) {
                throw new IllegalArgumentException("A load-balancer has already been set");
            }
            setLoadBalancerType(definition);
        }

        @JsonAlias("weighted")
        public  void setWeighted(WeightedLoadBalancerDefinition definition) {
            if (getLoadBalancerType() != null) {
                throw new IllegalArgumentException("A load-balancer has already been set");
            }
            setLoadBalancerType(definition);
        }

        @JsonAlias("roundRobin")
        public  void setRoundRobin(RoundRobinLoadBalancerDefinition definition) {
            if (getLoadBalancerType() != null) {
                throw new IllegalArgumentException("A load-balancer has already been set");
            }
            setLoadBalancerType(definition);
        }

        @JsonAlias("custom")
        public  void setCustom(CustomLoadBalancerDefinition definition) {
            setCustomLoadBalancer(definition);
        }

        public static final class Sticky extends StickyLoadBalancerDefinition implements HasExpression {
            @JsonIgnore
            @Override
            public void setCorrelationExpression(Expression expression) {
                super.setCorrelationExpression(expression);
            }

            @Override
            public void setExpression(ExpressionDefinition expressionDefinition) {
                super.setCorrelationExpression(expressionDefinition);
            }

            @Override
            public ExpressionDefinition getExpression() {
                final ExpressionSubElementDefinition expression = super.getCorrelationExpression();

                return expression != null
                    ? super.getCorrelationExpression().getExpressionType()
                    : null;
            }
        }
    }
}


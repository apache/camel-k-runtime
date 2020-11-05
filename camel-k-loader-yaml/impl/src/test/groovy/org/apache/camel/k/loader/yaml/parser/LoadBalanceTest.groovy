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
package org.apache.camel.k.loader.yaml.parser

import org.apache.camel.k.loader.yaml.support.TestSupport
import org.apache.camel.model.LoadBalanceDefinition
import org.apache.camel.model.loadbalancer.CustomLoadBalancerDefinition
import org.apache.camel.model.loadbalancer.FailoverLoadBalancerDefinition
import org.apache.camel.model.loadbalancer.RandomLoadBalancerDefinition
import org.apache.camel.model.loadbalancer.RoundRobinLoadBalancerDefinition
import org.apache.camel.model.loadbalancer.TopicLoadBalancerDefinition
import org.apache.camel.model.loadbalancer.WeightedLoadBalancerDefinition

class LoadBalanceTest extends TestSupport {

    def "random load balancer"() {
        when:
            def processor = toProcessor(LoadBalanceStepParser,'''
                 random: {}
            ''')
        then:
            with(processor, LoadBalanceDefinition) {
                loadBalancerType instanceof RandomLoadBalancerDefinition
            }
    }

    def "custom load balancer"() {
        when:
            def processor = toProcessor(LoadBalanceStepParser,'''
                 custom-load-balancer: 
                   ref: my-lb
            ''')
        then:
            with(processor, LoadBalanceDefinition) {
                with(loadBalancerType, CustomLoadBalancerDefinition) {
                    ref == 'my-lb'
                }
            }
    }

    def "custom load balancer (alias)"() {
        when:
            def processor = toProcessor(LoadBalanceStepParser,'''
                 custom: 
                   ref: my-lb
            ''')
        then:
            with(processor, LoadBalanceDefinition) {
                with(loadBalancerType, CustomLoadBalancerDefinition) {
                    ref == 'my-lb'
                }
            }
    }

    def "failover load balancer"() {
        when:
            def processor = toProcessor(LoadBalanceStepParser,'''
                 failover: 
                   exceptions: 
                     - java.lang.Exception
            ''')
        then:
            with(processor, LoadBalanceDefinition) {
                with(loadBalancerType, FailoverLoadBalancerDefinition) {
                    exceptions.size() == 1
                }
            }
    }

    def "sticky load balancer"() {
        when:
            def processor = toProcessor(LoadBalanceStepParser, '''
                 sticky: 
                   simple: '${header.id}'
            ''')
        then:
            with(processor, LoadBalanceDefinition) {
                with(loadBalancerType, LoadBalanceStepParser.Definition.Sticky) {
                    correlationExpression.expressionType.expression == '${header.id}'
                }
            }
    }

    def "sticky load balancer (expression)"() {
        when:
            def processor = toProcessor(LoadBalanceStepParser, '''
                 sticky: 
                   expression:
                     simple: '${header.id}'
            ''')
        then:
            with(processor, LoadBalanceDefinition) {
                with(loadBalancerType, LoadBalanceStepParser.Definition.Sticky) {
                    correlationExpression.expressionType.expression == '${header.id}'
                }
            }
    }

    def "topic load balancer"() {
        when:
            def processor = toProcessor(LoadBalanceStepParser, '''
                 topic: {}
            ''')
        then:
            with(processor, LoadBalanceDefinition) {
                loadBalancerType instanceof TopicLoadBalancerDefinition
            }
    }

    def "weighted load balancer"() {
        when:
            def processor = toProcessor(LoadBalanceStepParser, '''
                 weighted:
                   distribution-ratio: "1;2;3"
                   distribution-ratio-delimiter: ";"
            ''')
        then:
            with(processor, LoadBalanceDefinition) {
                with(loadBalancerType, WeightedLoadBalancerDefinition) {
                    distributionRatio == '1;2;3'
                    distributionRatioDelimiter == ';'
                }
            }
    }

    def "round-robin load balancer"() {
        when:
            def processor = toProcessor(LoadBalanceStepParser, '''
                 roundRobin: {}
            ''')
        then:
            with(processor, LoadBalanceDefinition) {
                loadBalancerType instanceof RoundRobinLoadBalancerDefinition
            }
    }

    def "round-robin load balancer (alias)"() {
        when:
            def processor = toProcessor(LoadBalanceStepParser, '''
                 round-robin: {}
            ''')
        then:
            with(processor, LoadBalanceDefinition) {
                loadBalancerType instanceof RoundRobinLoadBalancerDefinition
            }
    }

    def "load balancer (type)"() {
        when:
            def processor = toProcessor(LoadBalanceStepParser,'''
                 load-balancer-type:
                   random: {}
            ''')
        then:
            with(processor, LoadBalanceDefinition) {
                loadBalancerType instanceof RandomLoadBalancerDefinition
            }
    }

    def "load balancer (type alias)"() {
        when:
            def processor = toProcessor(LoadBalanceStepParser,'''
                 type:
                   random: {}
            ''')
        then:
            with(processor, LoadBalanceDefinition) {
                loadBalancerType instanceof RandomLoadBalancerDefinition
            }
    }

}

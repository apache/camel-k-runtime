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
import org.apache.camel.model.cloud.ServiceCallDefinition
import org.apache.camel.model.cloud.ServiceCallExpressionConfiguration
import org.apache.camel.model.language.ExpressionDefinition

class ServiceCallTest extends TestSupport {

    def "definition"() {
        when:
            def processor = toProcessor(ServiceCallStepParser, '''
                 name: "foo"
                 uri: "undertow:http://foo/hello"
            ''')
        then:
            with (processor, ServiceCallDefinition) {
                name == "foo"
                uri == "undertow:http://foo/hello"
            }
    }

    def "definition (inline)"() {
        when:
            def processor = toProcessor(ServiceCallStepParser, '''foo''')
        then:
            with (processor, ServiceCallDefinition) {
                name == "foo"
            }
    }

    def "definition with expression"() {
        when:
            def processor = toProcessor(ServiceCallStepParser, '''
                 expression: 
                    simple: "undertow:http://${header.service}/hello"
            ''')
        then:
            with (processor, ServiceCallDefinition) {
                expression != null
                with (expression, ExpressionDefinition) {
                    expression == 'undertow:http://${header.service}/hello'
                }
            }
    }

    def "definition with expression (inline)"() {
        when:
            def processor = toProcessor(ServiceCallStepParser, '''
                 simple: "undertow:http://${header.service}/hello"
            ''')
        then:
            with (processor, ServiceCallDefinition) {
                expression != null
                with (expression, ExpressionDefinition) {
                    expression == 'undertow:http://${header.service}/hello'
                }
            }
    }

    def "definition with configurations"() {
        when:
            def processor = toProcessor(ServiceCallStepParser,  '''
                 service-discovery-configuration: !org.apache.camel.model.cloud.StaticServiceCallServiceDiscoveryConfiguration
                     servers:
                         - "service1@host1"
                         - "service1@host2"                         
                 service-filter-configuration: !org.apache.camel.model.cloud.BlacklistServiceCallServiceFilterConfiguration
                     servers:
                         - "service2@host1"
            ''')
        then:
            with (processor, ServiceCallDefinition) {
                serviceDiscoveryConfiguration != null
                serviceFilterConfiguration != null
            }
    }

    def "definition with expression configuration"() {
        when:
            def processor = toProcessor(ServiceCallStepParser, '''
                 expression-configuration:
                     host-header: MyCamelServiceCallServiceHost
                     port-header: MyCamelServiceCallServicePort
                     expression: 
                        simple: "${header.service}"
            ''')
        then:
            with (processor, ServiceCallDefinition) {

                expressionConfiguration != null
                with (expressionConfiguration, ServiceCallExpressionConfiguration) {
                    hostHeader == 'MyCamelServiceCallServiceHost'
                    portHeader == 'MyCamelServiceCallServicePort'
                    expressionType.expression == '${header.service}'
                }
            }
    }

    def "definition with expression configuration (inline)"() {
        when:
            def processor = toProcessor(ServiceCallStepParser, '''
                 expression-configuration:
                     host-header: MyCamelServiceCallServiceHost
                     port-header: MyCamelServiceCallServicePort
                     simple: "${header.service}"
            ''')
        then:
            with (processor, ServiceCallDefinition) {

                expressionConfiguration != null
                with (expressionConfiguration, ServiceCallExpressionConfiguration) {
                    hostHeader == 'MyCamelServiceCallServiceHost'
                    portHeader == 'MyCamelServiceCallServicePort'
                    expressionType.expression == '${header.service}'
                }
            }
    }
}

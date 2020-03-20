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
package org.apache.camel.k.loader.yaml

import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.model.ChoiceDefinition
import org.apache.camel.model.FilterDefinition
import org.apache.camel.model.SplitDefinition
import org.apache.camel.model.ToDefinition
import org.apache.camel.model.language.SimpleExpression
import org.apache.camel.model.language.TokenizerExpression

class DefinitionsTest extends TestSupport {

    def "route with id"() {
        given:
            def content = '''
             - route:
                 id: my-route-id    
                 group: my-route-group 
                 from:
                     uri: "direct:start"
                     steps:
                       - to:
                           uri: "log:info"
            '''.stripMargin('|')

            def camelContext = new DefaultCamelContext()
        when:
            camelContext.addRoutes(new YamlSourceLoader().builder(content))
        then:
            camelContext.routeDefinitions[0].id == 'my-route-id'
            camelContext.routeDefinitions[0].group == 'my-route-group'
            camelContext.routeDefinitions[0].input.endpointUri == 'direct:start'

            with(camelContext.routeDefinitions[0].outputs[0], ToDefinition) {
                endpointUri == 'log:info'
            }
    }

    def "route with cbr"() {
        given:
            def content = '''
             - from:
                 uri: "direct:start"
                 steps:
                   - choice:
                       when:
                         - simple: "${body.startsWith(\\"a\\")}"
                           steps:
                             - to:
                                 uri: "log:when-a"
                         - expression:
                             simple: "${body.startsWith(\\"b\\")}"
                           steps:
                             - to:
                                 uri: "log:when-b"
                       otherwise:
                         steps:
                           - to:
                               uri: "log:otherwise"
                   - to:
                       uri: "log:info"
            '''.stripMargin('|')

            def camelContext = new DefaultCamelContext()
        when:
            camelContext.addRoutes(new YamlSourceLoader().builder(content))
        then:
            camelContext.routeDefinitions[0].input.endpointUri == 'direct:start'

            with(camelContext.routeDefinitions[0].outputs[0] as ChoiceDefinition) {
                whenClauses[0].expression.language == 'simple'
                whenClauses[0].expression.expression == '${body.startsWith("a")}'
                whenClauses[0].outputs.size() == 1

                with(whenClauses[0].outputs[0] as ToDefinition) {
                    endpointUri == 'log:when-a'
                }

                whenClauses[1].expression.language == 'simple'
                whenClauses[1].expression.expression == '${body.startsWith("b")}'
                whenClauses[1].outputs.size() == 1

                with(whenClauses[1].outputs[0] as ToDefinition) {
                    endpointUri == 'log:when-b'
                }

                otherwise.outputs.size() == 1
            }

            with(camelContext.routeDefinitions[0].outputs[1] as ToDefinition) {
                endpointUri == 'log:info'
            }
    }

    def "route with split"() {
        given:
            def content = '''
                 - from:
                     uri: "direct:start"
                     steps:
                       - split: 
                           tokenize: ","
                           steps:
                             - to: "log:split1"
                             - to: "log:split2"
                       - to: "log:info"
            '''.stripMargin('|')

            def camelContext = new DefaultCamelContext()
        when:
            camelContext.addRoutes(new YamlSourceLoader().builder(content))
        then:
            camelContext.routeDefinitions[0].input.endpointUri == 'direct:start'
            camelContext.routeDefinitions[0].outputs.size() == 2

            with(camelContext.routeDefinitions[0].outputs[0] as SplitDefinition) {
                with(expression as TokenizerExpression) {
                    token == ','
                }

                outputs.size() == 2

                with(outputs[0] as ToDefinition) {
                    endpointUri == 'log:split1'
                }
                with(outputs[1] as ToDefinition) {
                    endpointUri == 'log:split2'
                }
            }

            with(camelContext.routeDefinitions[0].outputs[1] as ToDefinition) {
                endpointUri == 'log:info'
            }
    }

    def "flow style route with split"() {
        given:
            def content = '''
                 - from:
                     uri: "direct:start"
                     steps:
                       - split: 
                           tokenize: ","
                       - to: "log:info"
            '''.stripMargin('|')

            def camelContext = new DefaultCamelContext()
        when:
            camelContext.addRoutes(new YamlSourceLoader().builder(content))
        then:
            camelContext.routeDefinitions[0].input.endpointUri == 'direct:start'
            camelContext.routeDefinitions[0].outputs.size() == 1

            with(camelContext.routeDefinitions[0].outputs[0] as SplitDefinition) {
                with(expression as TokenizerExpression) {
                    token == ','
                }

                outputs.size() == 1

                with(outputs[0] as ToDefinition) {
                    endpointUri == 'log:info'
                }
            }
    }

    def "route with filter"() {
        given:
            def content = '''
             - from:
                 uri: "direct:start"
                 steps:
                   - filter: 
                       simple: "${body.startsWith(\\"a\\")}"
                       steps:
                         - to: "log:filter1"
                         - to: "log:filter2"
                   - to: "log:info"
            '''.stripMargin('|')

            def camelContext = new DefaultCamelContext()
        when:
            camelContext.addRoutes(new YamlSourceLoader().builder(content))
        then:
            camelContext.routeDefinitions[0].input.endpointUri == 'direct:start'
            camelContext.routeDefinitions[0].outputs.size() == 2

            with(camelContext.routeDefinitions[0].outputs[0] as FilterDefinition) {
                with(expression as SimpleExpression) {
                    expression == '${body.startsWith("a")}'
                }

                outputs.size() == 2

                with(outputs[0] as ToDefinition) {
                    endpointUri == 'log:filter1'
                }
                with(outputs[1] as ToDefinition) {
                    endpointUri == 'log:filter2'
                }
            }

            with(camelContext.routeDefinitions[0].outputs[1] as ToDefinition) {
                endpointUri == 'log:info'
            }
    }

    def "flow style route with filter"() {
        given:
            def content = '''
             - from:
                 uri: "direct:start"
                 steps:
                   - filter: 
                       simple: "${body.startsWith(\\"a\\")}"
                   - to: "log:info"
            '''.stripMargin('|')

            def camelContext = new DefaultCamelContext()
        when:
            camelContext.addRoutes(new YamlSourceLoader().builder(content))
        then:
            camelContext.routeDefinitions[0].input.endpointUri == 'direct:start'
            camelContext.routeDefinitions[0].outputs.size() == 1

            with(camelContext.routeDefinitions[0].outputs[0] as FilterDefinition) {
                with(expression as SimpleExpression) {
                    expression == '${body.startsWith("a")}'
                }

                outputs.size() == 1

                with(outputs[0] as ToDefinition) {
                    endpointUri == 'log:info'
                }
            }
    }
}

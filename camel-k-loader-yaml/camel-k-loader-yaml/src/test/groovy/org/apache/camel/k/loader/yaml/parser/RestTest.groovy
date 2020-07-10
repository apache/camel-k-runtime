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

import org.apache.camel.k.loader.yaml.TestSupport
import org.apache.camel.k.loader.yaml.spi.StepParserException
import org.apache.camel.model.RouteDefinition
import org.apache.camel.model.rest.GetVerbDefinition

class RestTest extends TestSupport {

    def "definition"() {
        given:
            def stepContext = stepContext('''
                 verb: "get"
                 uri: "/api/{id}"
                 steps:
                   - log:
                       message: "test"
            ''')
        when:
            def processor = new RestStepParser().process(stepContext)
        then:
            with(processor, RouteDefinition) {
                restDefinition != null
                restDefinition.verbs[0] instanceof GetVerbDefinition
                restDefinition.verbs[0].uri == '/api/{id}'
            }
    }

    def "should fail without verb"() {
        given:
            def stepContext = stepContext('''
                 uri: "/api/{id}"
                 steps:
                   - log:
                       message: "test"
            ''')
        when:
            new RestStepParser().process(stepContext)
        then:
            def ex = thrown(StepParserException)

            ex.properties.contains('verb')
    }

    def "should fail without uri"() {
        given:
            def stepContext = stepContext('''
                 verb: "get"
                 steps:
                   - log:
                       message: "test"
            ''')
        when:
            new RestStepParser().process(stepContext)
        then:
            def ex = thrown(StepParserException)

            ex.properties.contains('uri')
    }

    def "should fail without steps"() {
        given:
            def stepContext = stepContext('''
                 verb: "get"
                 uri: "/api/{id}"
            ''')
        when:
            new RestStepParser().process(stepContext)
        then:
            def ex = thrown(StepParserException)

            ex.properties.contains('steps')
    }
}

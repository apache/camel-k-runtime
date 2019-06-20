/**
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
package org.apache.camel.k.yaml.parser

import org.apache.camel.k.yaml.TestSupport
import org.apache.camel.model.ChoiceDefinition

class ChoiceTest extends TestSupport {

    def "definition"() {
        given:
            def stepContext = stepContext('''
                 when:
                   - simple: "${body.size()} == 1"
                     steps:
                       - to:
                           uri: "log:when-a"
                   - expression:
                       simple: "${body.size()} == 2"
                     steps:
                       - to:
                           uri: "log:when-b"
                 otherwise:
                     steps:
                       - to:
                           uri: "log:otherwise"
            ''')
        when:
            def processor = new ChoiceStepParser().toProcessor(stepContext)
        then:
            with(processor, ChoiceDefinition) {
                whenClauses.size() == 2
                whenClauses[0].expression.language == 'simple'
                whenClauses[0].expression.expression == '${body.size()} == 1'
                whenClauses[0].outputs.size() == 1
                whenClauses[1].expression.language == 'simple'
                whenClauses[1].expression.expression == '${body.size()} == 2'
                whenClauses[1].outputs.size() == 1
                otherwise.outputs.size() == 1
            }
    }

    def "should fail without when"() {
        given:
            def stepContext = stepContext('''
                 otherwise:
                     steps:
                       - to:
                           uri: "log:otherwise"
            ''')
        when:
            new ChoiceStepParser().toProcessor(stepContext)
        then:
            def ex = thrown(StepParserException)

            ex.properties.contains('when')
    }

    def "should fail without when steps"() {
        given:
            def stepContext = stepContext('''
                 when:
                   - simple: "${body.size()} == 1"
                   - expression:
                       simple: "${body.size()} == 2"
            ''')
        when:
            new ChoiceStepParser().toProcessor(stepContext)
        then:
            def ex = thrown(StepParserException)

            ex.properties.contains('when.steps')
    }

    def "should fail without otherwise steps"() {
        given:
            def stepContext = stepContext('''
                 when:
                   - simple: "${body.size()} == 1"
                     steps:
                       - to:
                           uri: "log:when-a"
                 otherwise:
                   foo: "bar"
            ''')
        when:
            new ChoiceStepParser().toProcessor(stepContext)
        then:
            def ex = thrown(StepParserException)

            ex.properties.contains('otherwise.steps')
    }

}

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
import org.apache.camel.model.OnExceptionDefinition
import org.apache.camel.model.ToDefinition
import org.apache.camel.model.language.ConstantExpression

class OnExceptionTest extends TestSupport {
    def "definition (route)"() {
        given:
            def stepContext = stepContext('''
                 exceptions: 
                   - java.lang.Exception
                   - java.io.IOException
                 when:
                   constant: "when"
                   steps:
                     - to: 'log:when'
                 retry-while:                 
                   constant: "while"
                 handled:                 
                   constant: "handled"
                 continued:                 
                   constant: "continued"
                 
            ''')
        when:
            def processor = new OnExceptionStepParser().toProcessor(stepContext)
        then:
            with(processor, OnExceptionDefinition) {
                exceptions.contains('java.lang.Exception')
                exceptions.contains('java.io.IOException')

                with(onWhen) {
                    outputs.size() == 1

                    with(outputs[0], ToDefinition) {
                        endpointUri == 'log:when'
                    }
                    with(expression, ConstantExpression) {
                        expression == 'when'
                    }
                }
                with(retryWhile.expressionType, ConstantExpression) {
                    expression == 'while'
                }
                with(handled.expressionType, ConstantExpression) {
                    expression == 'handled'
                }
                with(continued.expressionType, ConstantExpression) {
                    expression == 'continued'
                }
            }
    }

    def "definition with maybe-booleans"() {
        when:
            def processor = toProcessor(OnExceptionStepParser, '''
                 handled: true
                 continued: false                 
            ''')
        then:
            with(processor, OnExceptionDefinition) {
                with(handled.expressionType, ConstantExpression) {
                    expression == 'true'
                }
                with(continued.expressionType, ConstantExpression) {
                    expression == 'false'
                }
            }
    }

    def "definition (global)"() {
        given:
            def stepContext = stepContext('''
                 exceptions: 
                   - java.lang.Exception
                   - java.io.IOException
                 when:
                   constant: "when"
                   steps:
                     - to: 'log:when'
                 retry-while:                 
                   constant: "while"
                 handled:                 
                   constant: "handled"
                 continued:                 
                   constant: "continued"
            ''')
        when:
            def processor = new OnExceptionStepParser().toStartProcessor(stepContext)
        then:
            stepContext.builder().routeCollection.onExceptions.size() == 1
            stepContext.builder().routeCollection.onExceptions[0].exceptions.contains("java.lang.Exception")
            stepContext.builder().routeCollection.onExceptions[0].exceptions.contains("java.io.IOException")

            with(processor, OnExceptionDefinition) {
                exceptions.contains('java.lang.Exception')
                exceptions.contains('java.io.IOException')

                with(onWhen) {
                    outputs.size() == 1

                    with(outputs[0], ToDefinition) {
                        endpointUri == 'log:when'
                    }
                    with(expression, ConstantExpression) {
                        expression == 'when'
                    }
                }
                with(retryWhile.expressionType, ConstantExpression) {
                    expression == 'while'
                }
                with(handled.expressionType, ConstantExpression) {
                    expression == 'handled'
                }
                with(continued.expressionType, ConstantExpression) {
                    expression == 'continued'
                }
            }
    }
}

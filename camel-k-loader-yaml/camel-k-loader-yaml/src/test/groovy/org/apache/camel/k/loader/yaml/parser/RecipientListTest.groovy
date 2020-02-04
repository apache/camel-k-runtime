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
import org.apache.camel.model.RecipientListDefinition
import org.apache.camel.model.language.ConstantExpression

class RecipientListTest extends TestSupport {

    def "definition with expression"() {
        given:
            def stepContext = stepContext('''
                constant: "direct:a,direct:b"
                stop-on-exception: true
                parallel-processing: true
            ''')
        when:
            def processor = new RecipientListStepParser().toProcessor(stepContext)
        then:
            with (processor, RecipientListDefinition) {
                stopOnException == 'true'
                parallelProcessing == 'true'

                with(expression, ConstantExpression) {
                    language == 'constant'
                    expression == 'direct:a,direct:b'
                }
            }
    }

    def "definition with expression block"() {
        given:
            def stepContext = stepContext('''
                expression:
                    constant: "direct:a,direct:b"
                stop-on-exception: true
                parallel-processing: true
            ''')
        when:
            def processor = new RecipientListStepParser().toProcessor(stepContext)
        then:
            with (processor, RecipientListDefinition) {
                stopOnException == 'true'
                parallelProcessing == 'true'

                with(expression, ConstantExpression) {
                    language == 'constant'
                    expression == 'direct:a,direct:b'
                }
            }
    }
}

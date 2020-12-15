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
import org.apache.camel.model.AggregateDefinition
import org.apache.camel.model.language.SimpleExpression

class AggregateTest extends TestSupport {

    def "definition"() {
        when:
            def processor = toProcessor('aggregate', '''
                 correlation-expression:
                     simple: "${header.Count}"
                 strategy-ref: "myAppender"
                 completion-size: 10
            ''')
        then:
            with(processor, AggregateDefinition) {
                strategyRef == 'myAppender'
                completionSize == '10'

                with(correlationExpression?.expressionType, SimpleExpression) {
                    expression == '${header.Count}'
                }
            }
    }

    def "compact definition"() {
        when:
            def processor = toProcessor('aggregate', '''
                 correlation-expression:
                     simple: "${header.Count}"
                 strategy-ref: "myAppender"
                 completion-size: 10
            ''')
        then:
            with(processor, AggregateDefinition) {
                strategyRef == 'myAppender'
                completionSize == '10'

                with(correlationExpression?.expressionType, SimpleExpression) {
                    expression == '${header.Count}'
                }
            }
    }
}

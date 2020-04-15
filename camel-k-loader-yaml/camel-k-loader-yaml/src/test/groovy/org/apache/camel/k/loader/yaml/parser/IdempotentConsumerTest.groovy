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
import org.apache.camel.model.IdempotentConsumerDefinition
import org.apache.camel.model.SplitDefinition

class IdempotentConsumerTest extends TestSupport {

    def "definition with expression"() {
        given:
            def stepContext = stepContext('''
                 simple: "${header.id}"
            ''')
        when:
            def processor = new IdempotentConsumerParser().toProcessor(stepContext)
        then:
            def p = processor as IdempotentConsumerDefinition

            p.expression.language == 'simple'
            p.expression.expression == '${header.id}'
    }

    def "definition with expression block"() {
        given:
            def stepContext = stepContext('''
                 expression:
                   simple: "${header.id}"
            ''')
        when:
            def processor = new IdempotentConsumerParser().toProcessor(stepContext)
        then:
            with(processor, IdempotentConsumerDefinition) {
                expression.language == 'simple'
                expression.expression == '${header.id}'
            }
    }

}

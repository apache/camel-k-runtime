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
import org.apache.camel.model.DelayDefinition

class DelayTest extends TestSupport {

    def "definition with expression"() {
        given:
            def stepContext = stepContext('''
                 simple: "${body}"
                 async-delayed: true
            ''')
        when:
            def processor = new DelayStepParser().toProcessor(stepContext)
        then:
            with(processor, DelayDefinition) {
                asyncDelayed == 'true'
                callerRunsWhenRejected == null
                expression.language == 'simple'
                expression.expression == '${body}'
            }
    }

    def "definition with expression block"() {
        given:
            def stepContext = stepContext('''
                 async-delayed: true
                 expression:
                   simple: "${body}"
            ''')
        when:
            def processor = new DelayStepParser().toProcessor(stepContext)
        then:
            with(processor, DelayDefinition) {
                asyncDelayed == 'true'
                callerRunsWhenRejected == null
                expression.language == 'simple'
                expression.expression == '${body}'
            }
    }

}

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
import org.apache.camel.model.WireTapDefinition
import org.apache.camel.model.language.ConstantExpression
import org.apache.camel.model.language.SimpleExpression

class WireTapTest extends TestSupport {

    def "definition"() {
        given:
            def stepContext = stepContext('''
                 uri: "direct:wt"
                 new-exchange:
                     simple: "${body}"
                     headers:
                         - name: "Header_1"
                           simple: "${header.MyHeader1}"
                         - name: "Header_2"
                           constant: "test"                 
                     body:
            ''')
        when:
            def processor = new WireTapStepParser().toProcessor(stepContext)
        then:
            with (processor, WireTapDefinition) {
                with (newExchangeExpression?.expression, SimpleExpression) {
                    language == 'simple'
                    expression == '${body}'
                }

                headers?.size() == 2

                with (headers[0].expression, SimpleExpression) {
                    language == 'simple'
                    expression == '${header.MyHeader1}'
                }
                with (headers[1].expression, ConstantExpression) {
                    language == 'constant'
                    expression == 'test'
                }
            }
    }
}

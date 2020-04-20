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
import org.apache.camel.model.SagaDefinition

class SagaTest extends TestSupport {

    def "definition"() {
        when:
            def processor = toProcessor(SagaStepParser, '''
                 propagation: "MANDATORY"
                 completion-mode: "MANUAL"
                 compensation: 
                     uri: "direct:compensation"
                 completion:
                     uri: "direct:completion"
                 steps:
                   - to: "direct:something"    
            ''')
        then:
            with (processor, SagaDefinition) {
                propagation == "MANDATORY"
                completionMode == "MANUAL"
                compensation.uri == "direct:compensation"
                completion.uri == "direct:completion"
                outputs.size() == 1
            }
    }

    def "definition short"() {
        when:
            def processor = toProcessor(SagaStepParser, '''
                 propagation: "MANDATORY"
                 completion-mode: "MANUAL"
                 compensation: "direct:compensation"
                 completion: "direct:completion"
                 steps:
                   - to: "direct:something"    
            ''')
        then:
            with (processor, SagaDefinition) {
                propagation == "MANDATORY"
                completionMode == "MANUAL"
                compensation.uri == "direct:compensation"
                completion.uri == "direct:completion"
                outputs.size() == 1
            }
    }
}

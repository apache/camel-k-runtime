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
import org.apache.camel.model.TryDefinition

class DoTryTest extends TestSupport {

    def "definition doCatch"() {
        given:
            def stepContext = stepContext('''
                 steps:
                   - to: "log:when-a"
                   - to: "log:when-b"
                 do-catch:
                     exceptions: 
                       - "java.io.FileNotFoundException"
                       - "java.io.IOException"
                     steps:
                       - to: "log:io-error"
            ''')
        when:
            def processor = new DoTryStepParser().toProcessor(stepContext)
        then:
            with(processor, TryDefinition) {
                outputs.size() == 3
                catchClauses.size() == 1
                catchClauses[0].outputs.size() == 1
                catchClauses[0].exceptions.size() == 2
                finallyClause == null
            }
    }

    def "definition doCatchOnWhen"() {
        given:
            def stepContext = stepContext('''
                 steps:
                   - to: "log:when-a"
                   - to: "log:when-b"
                 do-catch:
                     exceptions: 
                       - "java.io.FileNotFoundException"
                       - "java.io.IOException"
                     when:
                       simple: "${body.size()} == 1"
                     steps:
                       - to: "log:io-error"
            ''')
        when:
            def processor = new DoTryStepParser().toProcessor(stepContext)
        then:
            with(processor, TryDefinition) {
                outputs.size() == 3
                catchClauses.size() == 1
                catchClauses[0].outputs.size() == 1
                catchClauses[0].exceptions.size() == 2
                finallyClause == null
            }
    }

    def "definition doCatchOnWhenFinally"() {
        given:
        def stepContext = stepContext('''
                 steps:
                   - to: "log:when-a"
                   - to: "log:when-b"
                 do-catch:
                     exceptions: 
                       - "java.io.FileNotFoundException"
                       - "java.io.IOException"
                     when:
                       simple: "${body.size()} == 1"
                     steps:
                       - to: "log:io-error"
                 do-finally:
                   steps:
                     - to: "log:finally"
            ''')
        when:
        def processor = new DoTryStepParser().toProcessor(stepContext)
        then:
        with(processor, TryDefinition) {
            outputs.size() == 4
            catchClauses.size() == 1
            catchClauses[0].outputs.size() == 1
            catchClauses[0].exceptions.size() == 2
            finallyClause.outputs.size() == 1
        }
    }

    def "definition doCatchFinally"() {
        given:
        def stepContext = stepContext('''
                 steps:
                   - to: "log:when-a"
                   - to: "log:when-b"
                 do-catch:
                     exceptions: 
                       - "java.io.FileNotFoundException"
                       - "java.io.IOException"
                     steps:
                       - to: "log:io-error"
                 do-finally:
                   steps:
                     - to: "log:finally"
            ''')
        when:
        def processor = new DoTryStepParser().toProcessor(stepContext)
        then:
        with(processor, TryDefinition) {
            outputs.size() == 4
            catchClauses.size() == 1
            catchClauses[0].outputs.size() == 1
            catchClauses[0].exceptions.size() == 2
            finallyClause.outputs.size() == 1
        }
    }

    def "definition doFinally"() {
        given:
            def stepContext = stepContext('''
                 steps:
                   - to: "log:when-a"
                   - to: "log:when-b"
                 do-finally:
                   steps:
                     - to: "log:finally"
            ''')
        when:
            def processor = new DoTryStepParser().toProcessor(stepContext)
        then:
            with(processor, TryDefinition) {
                outputs.size() == 3
                catchClauses.size() == 0
                finallyClause.outputs.size() == 1
            }
    }

}

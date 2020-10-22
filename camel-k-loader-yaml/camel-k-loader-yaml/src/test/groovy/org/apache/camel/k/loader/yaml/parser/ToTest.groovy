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

import com.fasterxml.jackson.databind.node.TextNode
import org.apache.camel.k.loader.yaml.support.TestSupport
import org.apache.camel.model.ToDefinition

class ToTest extends TestSupport {

    def "definition"() {
        given:
            def stepContext = stepContext('''
                 uri: "seda:test"
                 parameters:
                   queueSize: 1
            ''')
        when:
            def processor = new ToStepParser().toProcessor(stepContext)
        then:
            with(processor, ToDefinition) {
                endpointUri ==~  /seda:(\/\/)?test\?queueSize=1/
            }
    }

    def "definition with secrets"() {
        given:
            def stepContext = stepContext('''
                 uri: "telegram://bots"
                 parameters:
                   authorizationToken: "s3cret"
            ''')
        when:
            def processor = new ToStepParser().toProcessor(stepContext)
        then:
            with(processor, ToDefinition) {
                endpointUri == 'telegram://bots?authorizationToken=RAW(s3cret)'
            }
    }

    def "definition with secrets (raw)"() {
        given:
            def stepContext = stepContext('''
                 uri: "telegram://bots"
                 parameters:
                   authorizationToken: "RAW(s3cret)"
            ''')
        when:
            def processor = new ToStepParser().toProcessor(stepContext)
        then:
            with(processor, ToDefinition) {
                endpointUri == 'telegram://bots?authorizationToken=RAW(s3cret)'
            }
    }

    def "definition with secrets (property)"() {
        given:
            def stepContext = stepContext('''
                 uri: "telegram://bots"
                 parameters:
                   authorizationToken: "#property:telegram.token"
            ''')
        when:
            def processor = new ToStepParser().toProcessor(stepContext)
        then:
            with(processor, ToDefinition) {
                endpointUri == 'telegram://bots?authorizationToken=#property:telegram.token'
            }
    }

    def "definition compact"() {
        given:
            def node = TextNode.valueOf('seda://test')
            def stepContext = stepContext(node)
        when:
            def processor = new ToStepParser().toProcessor(stepContext)
        then:
            with(processor, ToDefinition) {
                endpointUri ==~ /seda:(\/\/)?test/
            }
    }
}

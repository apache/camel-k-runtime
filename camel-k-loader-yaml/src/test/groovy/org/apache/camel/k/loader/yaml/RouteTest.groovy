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
package org.apache.camel.k.loader.yaml


import org.apache.camel.component.mock.MockEndpoint

class RouteTest extends TestSupport {

    def 'test split'() {
        setup:
            def context = startContext('''
                - from:
                    uri: "direct:route"
                    steps:
                      - split:
                          tokenizer: ","
                          steps:
                            - to: "mock:split"
                      - to: "mock:route"
                - from:
                    uri: "direct:flow"
                    steps:
                      - split:
                          tokenizer: ","
                      - to: "mock:flow"
            ''')

            mockEndpoint(context,'mock:split') {
                expectedMessageCount = 3
                expectedBodiesReceived 'a', 'b', 'c'
            }
            mockEndpoint(context,'mock:route') {
                expectedMessageCount = 1
                expectedBodiesReceived 'a,b,c'
            }
            mockEndpoint(context,'mock:flow') {
                expectedMessageCount = 3
                expectedBodiesReceived 'a', 'b', 'c'
            }
        when:
            context.createProducerTemplate().with {
                sendBody('direct:route', 'a,b,c')
                sendBody('direct:flow', 'a,b,c')
            }
        then:
            MockEndpoint.assertIsSatisfied(context)
        cleanup:
            context?.stop()
    }

    def 'test filter'() {
        setup:
            def context = startContext('''
                - from:
                    uri: "direct:route"
                    steps:
                      - filter:
                          simple: "${body.startsWith(\\"a\\")}"
                          steps:
                            - to: "mock:filter"
                      - to: "mock:route"
                - from:
                    uri: "direct:flow"
                    steps:
                      - filter:
                          simple: "${body.startsWith(\\"a\\")}"
                      - to: "mock:flow"
            ''')

            mockEndpoint(context, 'mock:route') {
                expectedMessageCount = 2
                expectedBodiesReceived 'a', 'b'
            }
            mockEndpoint(context, 'mock:filter') {
                expectedMessageCount = 1
                expectedBodiesReceived 'a'
            }
            mockEndpoint(context,'mock:flow') {
                expectedMessageCount = 1
                expectedBodiesReceived 'a'
            }
        when:
            context.createProducerTemplate().with {
                sendBody('direct:route', 'a')
                sendBody('direct:route', 'b')
                sendBody('direct:flow', 'a')
                sendBody('direct:flow', 'b')
            }
        then:
            MockEndpoint.assertIsSatisfied(context)
        cleanup:
            context?.stop()
    }
}

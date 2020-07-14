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
import org.apache.camel.k.loader.yaml.support.MyFailingProcessor
import org.apache.camel.processor.aggregate.UseLatestAggregationStrategy
import org.apache.camel.support.processor.idempotent.MemoryIdempotentRepository

class RoutesTest extends TestSupport {

    def 'split'() {
        setup:
            def context = startContext()

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

    def 'filter'() {
        setup:
            def context = startContext()

            mockEndpoint(context, 'mock:route') {
                expectedMessageCount 2
                expectedBodiesReceived 'a', 'b'
            }
            mockEndpoint(context, 'mock:filter') {
                expectedMessageCount 1
                expectedBodiesReceived 'a'
            }
            mockEndpoint(context,'mock:flow') {
                expectedMessageCount 1
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

    def 'aggregator'() {
        setup:
            def context = startContext {
                registry.bind('aggregatorStrategy', new UseLatestAggregationStrategy())
            }

            mockEndpoint(context, 'mock:route') {
                expectedMessageCount 2
                expectedBodiesReceived '2', '4'
            }
        when:
            context.createProducerTemplate().with {
                sendBodyAndHeader('direct:route', '1', 'StockSymbol', 1)
                sendBodyAndHeader('direct:route', '2', 'StockSymbol', 1)
                sendBodyAndHeader('direct:route', '3', 'StockSymbol', 2)
                sendBodyAndHeader('direct:route', '4', 'StockSymbol', 2)
            }
        then:
            MockEndpoint.assertIsSatisfied(context)
        cleanup:
            context?.stop()
    }

    def 'idempotentConsumer'() {
        setup:
            def context = startContext {
                registry.bind('myRepo', new MemoryIdempotentRepository())
            }

            mockEndpoint(context,'mock:idempotent') {
                expectedMessageCount = 3
                expectedBodiesReceived 'a', 'b', 'c'
            }
            mockEndpoint(context,'mock:route') {
                expectedMessageCount = 5
                expectedBodiesReceived 'a', 'b', 'a2', 'b2', 'c'
            }
        when:
            context.createProducerTemplate().with {
                sendBodyAndHeader('direct:route', 'a', 'id', '1')
                sendBodyAndHeader('direct:route', 'b', 'id', '2')
                sendBodyAndHeader('direct:route', 'a2', 'id', '1')
                sendBodyAndHeader('direct:route', 'b2', 'id', '2')
                sendBodyAndHeader('direct:route', 'c', 'id', '3')
            }
        then:
            MockEndpoint.assertIsSatisfied(context)
        cleanup:
            context?.stop()
    }

    def 'onExceptionHandled'() {
        setup:
            def context = startContext {
                registry.bind('myFailingProcessor', new MyFailingProcessor())
            }
        when:
            def out = context.createProducerTemplate().requestBody('direct:start', 'Hello World');
        then:
            out == 'Sorry'
        cleanup:
            context?.stop()
    }

    def 'errorHandler'() {
        setup:
            def context = startContext {
                registry.bind('myFailingProcessor', new MyFailingProcessor())
            }

            mockEndpoint(context, 'mock:on-error') {
                expectedMessageCount = 1
            }
        when:
            context.createProducerTemplate().requestBody('direct:start', 'Hello World');
        then:
            MockEndpoint.assertIsSatisfied(context)
        cleanup:
            context?.stop()
    }

    def 'bean'() {
        setup:
            def context = startContext()
        when:
            def out = context.createProducerTemplate().requestBody('direct:route', 'test');
        then:
            out == 'TEST'
        cleanup:
            context?.stop()
    }
}

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
            def context = startContextForSpec()

            mockEndpoint(context, 'mock:split') {
                expectedMessageCount = 3
                expectedBodiesReceived 'a', 'b', 'c'
            }
            mockEndpoint(context, 'mock:route') {
                expectedMessageCount = 1
                expectedBodiesReceived 'a,b,c'
            }
            mockEndpoint(context, 'mock:flow') {
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
            def context = startContextForSpec()

            mockEndpoint(context, 'mock:route') {
                expectedMessageCount 2
                expectedBodiesReceived 'a', 'b'
            }
            mockEndpoint(context, 'mock:filter') {
                expectedMessageCount 1
                expectedBodiesReceived 'a'
            }
            mockEndpoint(context, 'mock:flow') {
                expectedMessageCount 1
                expectedBodiesReceived 'a'
            }
        when:
            template(context).with {
                to('direct:route').withBody('a').send()
                to('direct:route').withBody('b').send()
                to('direct:flow').withBody('a').send()
                to('direct:flow').withBody('b').send()
            }
        then:
            MockEndpoint.assertIsSatisfied(context)
        cleanup:
            context?.stop()
    }

    def 'aggregator'() {
        setup:
            def context = startContextForSpec {
                registry.bind('aggregatorStrategy', new UseLatestAggregationStrategy())
            }

            mockEndpoint(context, 'mock:route') {
                expectedMessageCount 2
                expectedBodiesReceived '2', '4'
            }
        when:
            template(context).with {
                to('direct:route').withBody('1').withHeader('StockSymbol', 1).send()
                to('direct:route').withBody('2').withHeader('StockSymbol', 1).send()
                to('direct:route').withBody('3').withHeader('StockSymbol', 2).send()
                to('direct:route').withBody('4').withHeader('StockSymbol', 2).send()
            }
        then:
            MockEndpoint.assertIsSatisfied(context)
        cleanup:
            context?.stop()
    }

    def 'idempotentConsumer'() {
        setup:
            def context = startContextForSpec {
                registry.bind('myRepo', new MemoryIdempotentRepository())
            }

            mockEndpoint(context, 'mock:idempotent') {
                expectedMessageCount = 3
                expectedBodiesReceived 'a', 'b', 'c'
            }
            mockEndpoint(context, 'mock:route') {
                expectedMessageCount = 5
                expectedBodiesReceived 'a', 'b', 'a2', 'b2', 'c'
            }
        when:
            template(context).with {
                to('direct:route').withBody('a').withHeader('id', '1').send()
                to('direct:route').withBody('b').withHeader('id', '2').send()
                to('direct:route').withBody('a2').withHeader('id', '1').send()
                to('direct:route').withBody('b2').withHeader('id', '2').send()
                to('direct:route').withBody('c').withHeader('id', '3').send()
            }
        then:
            MockEndpoint.assertIsSatisfied(context)
        cleanup:
            context?.stop()
    }

    def 'onExceptionHandled'() {
        setup:
            def context = startContextForSpec {
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
            def context = startContextForSpec {
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
            def context = startContextForSpec()
        when:
            def out = context.createProducerTemplate().requestBody('direct:route', 'test');
        then:
            out == 'TEST'
        cleanup:
            context?.stop()
    }
}

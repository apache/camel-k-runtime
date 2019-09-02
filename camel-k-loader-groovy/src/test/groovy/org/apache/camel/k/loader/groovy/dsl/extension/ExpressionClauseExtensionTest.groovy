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
package org.apache.camel.k.loader.groovy.dsl.extension

import org.apache.camel.builder.RouteBuilder
import org.apache.camel.impl.DefaultCamelContext
import spock.lang.Specification

class ExpressionClauseExtensionTest extends Specification {

    def "invoke extension method - untyped body expression"()  {
        given:
            def ctx = new DefaultCamelContext()
            ctx.addRoutes(new RouteBuilder() {
                @Override
                void configure() throws Exception {
                    from('direct:start')
                        .transform().body { it.toString() }
                }
            })

            ctx.start()
        when:
            def result = ctx.createProducerTemplate().requestBody('direct:start', 1)

        then:
            result instanceof String

        cleanup:
            ctx.stop()
    }

    def "invoke extension method - typed body expression"()  {
        given:
            def ctx = new DefaultCamelContext()
            ctx.addRoutes(new RouteBuilder() {
                @Override
                void configure() throws Exception {
                    from('direct:start')
                        .transform().body(String.class, { it.toUpperCase() })
                }
            })

            ctx.start()
        when:
            def result = ctx.createProducerTemplate().requestBody('direct:start', 'a')

        then:
            result instanceof String
            result == 'A'

        cleanup:
            ctx.stop()
    }

    def "invoke extension method - message expression"()  {
        given:
            def ctx = new DefaultCamelContext()
            ctx.addRoutes(new RouteBuilder() {
                @Override
                void configure() throws Exception {
                    from('direct:start')
                        .transform().message { it.body.toUpperCase() }
                }
            })

            ctx.start()
        when:
            def result = ctx.createProducerTemplate().requestBody('direct:start', 'a')

        then:
            result instanceof String
            result == 'A'

        cleanup:
            ctx.stop()
    }

    def "invoke extension method - cbr"()  {
        given:
            def ctx = new DefaultCamelContext()
            ctx.addRoutes(new RouteBuilder() {
                @Override
                void configure() throws Exception {
                    from('direct:start')
                        .choice()
                            .when().body(String.class, { it == '1'})
                                .setBody().constant('case-1')
                                .endChoice()
                            .when().body(String.class, { it == '2'})
                                .setBody().constant('case-2')
                                .endChoice()
                            .otherwise()
                                .setBody().constant('default')
                        .end()
                }
            })

            ctx.start()
        when:
            def r1 = ctx.createProducerTemplate().requestBody('direct:start', '1')
            def r2 = ctx.createProducerTemplate().requestBody('direct:start', '2')
            def r3 = ctx.createProducerTemplate().requestBody('direct:start', '3')

        then:
            r1 instanceof String
            r1 == 'case-1'
            r2 instanceof String
            r2 == 'case-2'
            r3 instanceof String
            r3 == 'default'

        cleanup:
            ctx.stop()
    }
}

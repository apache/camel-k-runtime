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
package org.apache.camel.k.loader.groovy

import org.apache.camel.CamelContext
import org.apache.camel.RoutesBuilder
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.k.Runtime
import org.apache.camel.k.Sources
import org.apache.camel.k.listener.RoutesConfigurer
import org.apache.camel.model.FromDefinition
import org.apache.camel.model.ToDefinition
import spock.lang.Specification

class LoaderTest extends Specification {

    def "load routes"() {
        given:
            def runtime = new TestRuntime()
            def source = Sources.fromURI("classpath:routes.groovy")

        when:
            def loader = RoutesConfigurer.load(runtime, source)

        then:
            loader instanceof GroovySourceLoader
            runtime.builders.size() == 1
            runtime.builders[0] instanceof RouteBuilder

            with(runtime.builders[0], RouteBuilder) {
                it.setContext(runtime.camelContext)
                it.configure()

                def routes = it.routeCollection.routes

                routes.size() == 1
                routes[0].outputs[0] instanceof ToDefinition
                routes[0].input.endpointUri == 'timer:tick'
            }
    }

    def "load routes with endpoint dsl"() {
        given:
            def runtime = new TestRuntime()
            def source = Sources.fromURI("classpath:routes-with-endpoint-dsl.groovy")

        when:
            def loader = RoutesConfigurer.load(runtime, source)

        then:
            loader instanceof GroovySourceLoader
            runtime.builders.size() == 1

            with(runtime.builders[0], RouteBuilder) {
                it.setContext(runtime.camelContext)
                it.configure()

                def routes = it.routeCollection.routes
                routes.size() == 1

                with(routes[0].input, FromDefinition) {
                    it.endpointUri == 'timer:tick?period=1s'
                }
                with(routes[0].outputs[0], ToDefinition) {
                    it.endpointUri == 'log:info'
                }
            }
    }

    static class TestRuntime implements Runtime {
        private final CamelContext camelContext
        private final List<RoutesBuilder> builders

        TestRuntime() {
            this.camelContext = new DefaultCamelContext()
            this.builders = new ArrayList<>()
        }

        @Override
        CamelContext getCamelContext() {
            return this.camelContext
        }

        @Override
        void addRoutes(RoutesBuilder builder) {
            this.builders.add(builder)
        }
    }
}

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

import org.apache.camel.component.direct.DirectEndpoint
import org.apache.camel.component.log.LogEndpoint
import org.apache.camel.k.loader.yaml.support.TestRuntime
import org.apache.camel.model.ToDefinition
import org.apache.camel.model.ToDynamicDefinition
import spock.lang.AutoCleanup
import spock.lang.Specification

class YamlSourceLoaderTest extends Specification {
    @AutoCleanup
    def runtime = new TestRuntime()

    def "to with parameters"() {
        expect:
            runtime.loadRoutes('classpath:yaml/routes_to.yaml')
            runtime.start()

            with(runtime.context.routeDefinitions) {
                it[0].input.endpointUri ==~ /direct:.*start/
                it[0].outputs[0] instanceof ToDefinition
            }
            with(runtime.context.endpoints.find {it instanceof LogEndpoint}, LogEndpoint) {
                it.showAll
                it.multiline
            }
    }

    def "to with out of order parameters"() {
        expect:
            runtime.loadRoutes('classpath:yaml/routes_to_out_of_order.yaml')
            runtime.start()

            with(runtime.context.routeDefinitions) {
                it[0].input.endpointUri ==~ /direct:.*start/
                it[0].outputs[0] instanceof ToDefinition
            }
            with(runtime.context.endpoints.find {it instanceof LogEndpoint}, LogEndpoint) {
                it.showAll
                it.multiline
            }
    }

    def "to dynamic with parameters"() {
        expect:
            runtime.loadRoutes('classpath:yaml/routes_to_dynamic.yaml')
            runtime.start()

            with(runtime.context.routeDefinitions) {
                it[0].input.endpointUri ==~ /direct:.*start/
                with (it[0].outputs[0], ToDynamicDefinition) {
                    it.uri == 'log:info?multiline=true&showAll=true'
                }
            }
    }

    def "to dynamic with out of order parameters"() {
        expect:
            runtime.loadRoutes('classpath:yaml/routes_to_dynamic_out_of_order.yaml')
            runtime.start()

            with(runtime.context.routeDefinitions) {
                it[0].input.endpointUri ==~ /direct:.*start/
                with (it[0].outputs[0], ToDynamicDefinition) {
                    it.uri == 'log:info?multiline=true&showAll=true'
                }
            }
    }

    def "to dynamic alias with parameters"() {
        expect:
            runtime.loadRoutes('classpath:yaml/routes_to_dynamic_alias.yaml')
            runtime.start()

            with(runtime.context.routeDefinitions) {
                it[0].input.endpointUri ==~ /direct:.*start/
                with (it[0].outputs[0], ToDynamicDefinition) {
                    it.uri == 'log:info?multiline=true&showAll=true'
                }
            }
    }

    def "to dynamic alias with out of order parameters"() {
        expect:
            runtime.loadRoutes('classpath:yaml/routes_to_dynamic_alias_out_of_order.yaml')
            runtime.start()

            with(runtime.context.routeDefinitions) {
                it[0].input.endpointUri ==~ /direct:.*start/
                with (it[0].outputs[0], ToDynamicDefinition) {
                    it.uri == 'log:info?multiline=true&showAll=true'
                }
            }
    }

    def "from with parameters"() {
        expect:
            runtime.loadRoutes('classpath:yaml/routes_from.yaml')
            runtime.start()

            with(runtime.context.routeDefinitions) {
                it[0].input.endpointUri ==~ /direct:.*start\?.*/
                it[0].outputs[0] instanceof ToDefinition
            }
            with(runtime.context.endpoints.find {it instanceof DirectEndpoint}, DirectEndpoint) {
                it.timeout == 1234L
            }
    }

    def "route with parameters"() {
        expect:
            runtime.loadRoutes('classpath:yaml/routes.yaml')
            runtime.start()

            with(runtime.context.routeDefinitions) {
                it[0].input.endpointUri ==~ /direct:.*start\?.*/
                it[0].outputs[0] instanceof ToDefinition
            }
            with(runtime.context.endpoints.find {it instanceof DirectEndpoint}, DirectEndpoint) {
                it.timeout == 1234L
            }
    }

    def "all"() {
        expect:
            runtime.loadRoutes('classpath:yaml/routes_all.yaml')
            runtime.start()

            with(runtime.context.routeDefinitions) {
                it[0].input.endpointUri ==~ /direct:.*start\?.*/
                it[0].outputs[0] instanceof ToDefinition
            }
            with(runtime.context.endpoints.find {it instanceof DirectEndpoint}, DirectEndpoint) {
                it.timeout == 1234L
            }
            with(runtime.context.endpoints.find {it instanceof LogEndpoint}, LogEndpoint) {
                it.showAll
                it.multiline
            }
    }
}

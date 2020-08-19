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
package org.apache.camel.k.loader.js

import org.apache.camel.component.seda.SedaComponent
import org.apache.camel.k.loader.js.support.TestRuntime
import org.apache.camel.model.FromDefinition
import org.apache.camel.model.ToDefinition
import org.apache.camel.model.TransformDefinition
import org.apache.camel.model.rest.GetVerbDefinition
import spock.lang.AutoCleanup
import spock.lang.Specification

class JavaScriptSourceLoaderTest extends Specification {
    @AutoCleanup
    def runtime = new TestRuntime()

    def 'load'(location) {
        expect:
            runtime.loadRoutes(location)

            with(runtime.context.routeDefinitions) {
                it[0].input.endpointUri ==~ /timer:.*tick/
                it[0].outputs[0] instanceof ToDefinition
            }
        where:
            location << [
                    'classpath:routes.js',
                    'classpath:routes-with-endpoint-dsl.js',
                    'classpath:routes-compressed.js.gz.b64?language=js&compression=true',
                    'classpath:routes.mytype?language=js'
            ]

    }

    def 'load routes with component configuration'() {
        when:
            runtime.loadRoutes('classpath:routes-with-component-configuration.js')
        then:
            with(runtime.context.getComponent('seda', SedaComponent.class)) {
                queueSize == 1234
            }
    }

    def 'load routes with rest configuration'() {
        when:
            runtime.loadRoutes('classpath:routes-with-rest-configuration.js')
        then:
            with(runtime.context.restConfiguration) {
                component == 'undertow'
                port == 1234
            }
    }

    def 'load routes with rest dsl'() {
        when:
            runtime.loadRoutes('classpath:routes-with-rest-dsl.js')
        then:
            runtime.context.restDefinitions.size() == 1
            runtime.context.routeDefinitions.size() == 1

            with(runtime.context.restDefinitions[0]) {
                produces == 'text/plain'
                with(verbs[0], GetVerbDefinition) {
                    uri == '/say/hello'
                }
            }
            with(runtime.context.routeDefinitions[0]) {
                input instanceof FromDefinition
                outputs[0] instanceof TransformDefinition
            }
    }

    def 'load routes with processors'() {
        when:
            runtime.loadRoutes('classpath:routes-with-processors.js')
            runtime.start()
        then:
            'arrow' == runtime.template.to('direct:arrow').request(String.class)
            'wrapper' == runtime.template.to('direct:wrapper').request(String.class)
            'function' == runtime.template.to('direct:function').request(String.class)
    }

    def 'load routes with context configuration'() {
        when:
            runtime.loadRoutes('classpath:routes-with-context-configuration.js')
        then:
            runtime.context.isTypeConverterStatisticsEnabled()

    }
}

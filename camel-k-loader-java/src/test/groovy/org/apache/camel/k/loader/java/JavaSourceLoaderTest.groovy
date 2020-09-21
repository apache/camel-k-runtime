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
package org.apache.camel.k.loader.java


import org.apache.camel.k.loader.java.model.EmployeeDTO
import org.apache.camel.k.loader.java.support.TestRuntime
import org.apache.camel.model.ProcessDefinition
import org.apache.camel.model.SetBodyDefinition
import org.apache.camel.model.ToDefinition
import spock.lang.AutoCleanup
import spock.lang.Specification

class JavaSourceLoaderTest extends Specification {
    @AutoCleanup
    def runtime = new TestRuntime()

    def "load routes with nested class"() {
        when:
            runtime.loadRoutes("classpath:MyRoutesWithNestedClass.java")
        then:
            with(runtime.context.routeDefinitions) {
                it.size() == 1

                it[0].outputs[0] instanceof SetBodyDefinition
                it[0].outputs[1] instanceof ProcessDefinition
                it[0].outputs[2] instanceof ToDefinition

                it[0].input.endpointUri == 'timer:tick'
            }
    }

    def "load routes with nested type"() {
        when:
            runtime.loadRoutes("classpath:MyRoutesWithNestedTypes.java")
            runtime.context.applicationContextClassLoader.loadClass('MyRoutesWithNestedTypes$MyModel')
        then:
            noExceptionThrown()
    }

    def "load routes with rest configuration"() {
        when:
            runtime.loadRoutes("classpath:MyRoutesWithRestConfiguration.java")
        then:
            runtime.context.restConfiguration.component == 'restlet'
    }

    def "load routes with model"() {
        when:
            runtime.loadRoutes("classpath:MyRoutesWithModel.java")
        then:
            runtime.context.restDefinitions.any {
                it.verbs.first().outType == EmployeeDTO.class.name
            }
    }

    def "load"(location) {
        expect:
            runtime.loadRoutes(location)

            with(runtime.context.routeDefinitions) {
                it[0].input.endpointUri ==~ /timer:.*tick/
                it[0].outputs[0] instanceof ToDefinition
            }
        where:
            location << [
                "classpath:MyRoutes.java",
                "classpath:MyRoutesWithNameOverride.java?name=MyRoutes.java",
                "classpath:MyRoutesWithPackage.java",
                "classpath:MyRoutesWithPackageAndComment.java",
                "classpath:MyRoutesWithPackageAndLineComment.java",
                "classpath:MyRoutesWithEndpointDsl.java"
            ]

    }
}

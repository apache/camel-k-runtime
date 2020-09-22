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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.github.fge.jackson.JsonLoader
import com.github.fge.jsonschema.main.JsonSchemaFactory
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class RoutesValidationTest extends Specification {
    static def MAPPER = new ObjectMapper(new YAMLFactory())
    static def SCHEMA_RES = JsonLoader.fromResource('/camel-yaml-dsl.json')
    static def SCHEMA = JsonSchemaFactory.byDefault().getJsonSchema(SCHEMA_RES)

    @Unroll
    def 'validate #source.last()'(Path source) {
        given:
            def target = MAPPER.readTree(source.toFile())
        when:
            def report = SCHEMA.validate(target)
        then:
            report.isSuccess()
        where:
            source << routes()
    }

    def routes() {
        def routes = getClass().getResource("/routes").toURI()
        def paths = Paths.get(routes)

        return Files.list(paths)
            // exclude RouteWithEndpointTest_ as there's no Endpoint DSL integration
            // with the json schema
            .filter(p -> !p.last().toString().startsWith("RoutesWithEndpointTest_"))
    }
}

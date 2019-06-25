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
package org.apache.camel.k.yaml

import com.fasterxml.jackson.databind.JsonNode
import groovy.util.logging.Slf4j
import org.apache.camel.CamelContext
import org.apache.camel.component.mock.MockEndpoint
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.k.yaml.parser.StepParser
import org.apache.commons.io.IOUtils
import spock.lang.Specification

import java.nio.charset.StandardCharsets

@Slf4j
class TestSupport extends Specification {

    static StepParser.Context stepContext(String content) {
        def node = Yaml.MAPPER.readTree(content.stripMargin())
        def cctx = new DefaultCamelContext()

        return new StepParser.Context(cctx, node)
    }

    static StepParser.Context stepContext(JsonNode content) {
        def cctx = new DefaultCamelContext()

        return new StepParser.Context(cctx, content)
    }

    static CamelContext startContext(String content) {
        def context = new DefaultCamelContext()
        def istream = IOUtils.toInputStream(content.stripMargin(), StandardCharsets.UTF_8)
        def builder = YamlRoutesLoader.builder(istream)

        context.disableJMX()
        context.setStreamCaching(true)
        context.addRoutes(builder)
        context.start()

        return context
    }

    static MockEndpoint mockEndpoint(
            CamelContext context,
            String uri,
            @DelegatesTo(MockEndpoint) Closure<MockEndpoint> closure) {

        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure.delegate = context.getEndpoint(uri, MockEndpoint.class)
        closure.call()

        return closure.delegate
    }
}

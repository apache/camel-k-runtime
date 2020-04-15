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

import com.fasterxml.jackson.databind.JsonNode
import groovy.util.logging.Slf4j
import org.apache.camel.CamelContext
import org.apache.camel.component.mock.MockEndpoint
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.k.loader.yaml.parser.ProcessorStepParser
import org.apache.camel.k.loader.yaml.parser.StepParser
import org.apache.camel.model.ProcessorDefinition
import spock.lang.Specification

import java.nio.charset.StandardCharsets

@Slf4j
class TestSupport extends Specification {
    static def MAPPER = new YamlSourceLoader().mapper()

    static StepParser.Context stepContext(String content) {
        def node = MAPPER.readTree(content.stripMargin())
        def cctx = new DefaultCamelContext()

        return new StepParser.Context(cctx, MAPPER, node)
    }

    static StepParser.Context stepContext(JsonNode content) {
        return new StepParser.Context(new DefaultCamelContext(), MAPPER, content)
    }

    static CamelContext startContext(String content) {
        return startContext(content, [:])
    }

    static CamelContext startContext(String content, Map<String, Object> beans) {
        return startContext(
                new ByteArrayInputStream(content.stripMargin().getBytes(StandardCharsets.UTF_8)),
                beans
        )
    }

    static CamelContext startContext(InputStream content) {
        return startContext(content, [:])
    }

    static CamelContext startContext(InputStream content, Map<String, Object> beans) {
        def context = new DefaultCamelContext()
        def builder = new YamlSourceLoader().builder(content)

        if (beans) {
            beans.each {
                k, v -> context.registry.bind(k, v)
            }
        }

        context.disableJMX()
        context.setStreamCaching(true)
        context.addRoutes(builder)
        context.start()

        return context
    }

    CamelContext startContext() {
        return startContext([:])
    }

    CamelContext startContext(Map<String, Object> beans) {
        def name = specificationContext.currentIteration.name.replace(' ', '_')
        def path = "/routes/${specificationContext.currentSpec.name}_${name}.yaml"

        return startContext(
                TestSupport.class.getResourceAsStream(path) as InputStream,
                beans
        )
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

    static <U extends ProcessorStepParser> ProcessorDefinition<?> toProcessor(Class<U> type, String content) {
        return type.getConstructor().newInstance().toProcessor(stepContext(content))
    }
}

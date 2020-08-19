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
package org.apache.camel.k.loader.yaml.support

import com.fasterxml.jackson.databind.JsonNode
import groovy.util.logging.Slf4j
import org.apache.camel.CamelContext
import org.apache.camel.FluentProducerTemplate
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.component.mock.MockEndpoint
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.k.Runtime
import org.apache.camel.k.Source
import org.apache.camel.k.Sources
import org.apache.camel.k.loader.yaml.YamlSourceLoader
import org.apache.camel.k.loader.yaml.YamlStepResolver
import org.apache.camel.k.loader.yaml.spi.ProcessorStepParser
import org.apache.camel.k.loader.yaml.spi.StartStepParser
import org.apache.camel.k.loader.yaml.spi.StepParser
import org.apache.camel.model.ProcessorDefinition
import org.apache.camel.model.RouteDefinition
import spock.lang.Specification

import java.nio.charset.StandardCharsets

@Slf4j
class TestSupport extends Specification {
    static def RESOLVER =  new YamlStepResolver()
    static def MAPPER = new YamlSourceLoader().mapper()

    static StepParser.Context stepContext(String content) {
        def node = MAPPER.readTree(content.stripMargin())
        def builder = new RouteBuilder(new DefaultCamelContext()) {
            @Override
            void configure() throws Exception {
            }
        }

        return new StepParser.Context(builder, new RouteDefinition(), MAPPER, node, RESOLVER)
    }

    static StepParser.Context stepContext(JsonNode content) {
        def builder = new RouteBuilder(new DefaultCamelContext()) {
            @Override
            void configure() throws Exception {
            }
        }

        return new StepParser.Context(builder, new RouteDefinition(), MAPPER, content, RESOLVER)
    }

    static CamelContext startContext(
            String content,
            @DelegatesTo(CamelContext) Closure<CamelContext> closure) {
        return startContext(
                Sources.fromBytes('yaml', content.getBytes(StandardCharsets.UTF_8)),
                closure
        )
    }

    static CamelContext startContext(
            Source source,
            @DelegatesTo(CamelContext) Closure closure) {
        def context = new DefaultCamelContext()
        def builder = new YamlSourceLoader().load(Runtime.on(context), source).builder().orElseThrow(() -> new IllegalArgumentException());

        context.disableJMX()
        context.setStreamCaching(true)

        if (closure) {
            closure.resolveStrategy = Closure.DELEGATE_ONLY
            closure.delegate = context
            closure.call()
        }

        context.addRoutes(builder)
        context.start()

        return context
    }

    CamelContext startContextForSpec(@DelegatesTo(CamelContext) Closure closure) {
        def name = specificationContext.currentIteration.name.replace(' ', '_')
        def path = "classpath:/routes/${specificationContext.currentSpec.name}_${name}.yaml"

        return startContext(
                Sources.fromURI(path),
                closure
        )
    }

    static mockEndpoint(
            CamelContext context,
            String uri,
            @DelegatesTo(MockEndpoint) Closure<?> closure) {

        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure.delegate = context.getEndpoint(uri, MockEndpoint.class)
        closure.call()
    }

    static <U extends ProcessorStepParser> ProcessorDefinition<?> toProcessor(Class<U> type, String content) {
        return type.getConstructor().newInstance().toProcessor(stepContext(content))
    }

    static <U extends StartStepParser> ProcessorDefinition<?> toStartProcessor(Class<U> type, String content) {
        return type.getConstructor().newInstance().process(stepContext(content))
    }

    static ProcessorDefinition<?> toProcessor(String id, String content) {
        def ctx = stepContext(content)
        def parser = RESOLVER.resolve(ctx.camelContext, id)

        if (parser instanceof ProcessorStepParser) {
            return parser.toProcessor(ctx)
        }
        if (parser instanceof StartStepParser) {
            return parser.process(ctx)
        }
        throw new IllegalArgumentException("No parser of ${id}")
    }

    static FluentProducerTemplate template(CamelContext context) {
        return context.createFluentProducerTemplate()
    }

    static def load(CamelContext context, String content) {
        def source = Sources.fromBytes('yaml', content.stripMargin().getBytes(StandardCharsets.UTF_8))
        def builder = new YamlSourceLoader().load(Runtime.on(context), source).builder().orElseThrow(() -> new IllegalArgumentException());

        context.addRoutes(builder)
    }
}

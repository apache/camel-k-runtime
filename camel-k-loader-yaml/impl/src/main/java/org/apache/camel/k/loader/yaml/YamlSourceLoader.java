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
package org.apache.camel.k.loader.yaml;

import java.io.Reader;
import java.util.Collection;
import java.util.Collections;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.k.Source;
import org.apache.camel.k.SourceLoader;
import org.apache.camel.k.annotation.Loader;
import org.apache.camel.k.loader.yaml.model.Step;
import org.apache.camel.k.loader.yaml.spi.StartStepParser;
import org.apache.camel.k.loader.yaml.spi.StepParser;
import org.apache.camel.k.loader.yaml.spi.TypeResolver;
import org.apache.camel.k.support.RouteBuilders;
import org.apache.camel.util.function.ThrowingBiConsumer;

@Loader("yaml")
public class YamlSourceLoader implements SourceLoader {
    public static final StepParser.Resolver STEP_RESOLVER;
    public static final TypeResolver TYPE_RESOLVER;
    public static final ObjectMapper MAPPER;

    static {
        // register custom reifiers auto-generated from the step parser definitions
        YamlReifiers.registerReifiers();

        // Use a global caching resolver
        STEP_RESOLVER = StepParser.Resolver.caching(new YamlStepResolver());

        // Use a global caching type resolver
        TYPE_RESOLVER = TypeResolver.caching(new YamlTypeResolver());

        // Create the object mapper
        MAPPER = new ObjectMapper(
            new YAMLFactory()
                .configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, true)
                .configure(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID, false))
            .registerModule(new YamlModule())
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
            .setPropertyNamingStrategy(PropertyNamingStrategy.KEBAB_CASE)
            .enable(MapperFeature.USE_GETTERS_AS_SETTERS)
            .disable(MapperFeature.REQUIRE_SETTERS_FOR_GETTERS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Override
    public Collection<String> getSupportedLanguages() {
        return Collections.singletonList("yaml");
    }

    @Override
    public RoutesBuilder load(CamelContext camelContext, Source source) {
        final ObjectReader objectReader = MAPPER.readerForArrayOf(Step.class)
            .withAttribute(StepParser.Resolver.class, STEP_RESOLVER)
            .withAttribute(TypeResolver.class, TYPE_RESOLVER)
            .withAttribute(CamelContext.class, camelContext);

        return RouteBuilders.route(source, new ThrowingBiConsumer<Reader, RouteBuilder, Exception>() {
            @Override
            public void accept(Reader reader, RouteBuilder builder) throws Exception {
                for (Step step : (Step[])objectReader.readValue(reader)) {
                    StartStepParser.invoke(
                        new StepParser.Context(builder, null, MAPPER, step.node, STEP_RESOLVER),
                        step.id);
                }
            }
        });
    }
}

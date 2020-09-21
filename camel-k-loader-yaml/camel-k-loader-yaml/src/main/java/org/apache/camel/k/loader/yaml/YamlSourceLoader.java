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

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.k.Runtime;
import org.apache.camel.k.Source;
import org.apache.camel.k.SourceLoader;
import org.apache.camel.k.annotation.Loader;
import org.apache.camel.k.loader.yaml.model.Step;
import org.apache.camel.k.loader.yaml.spi.StartStepParser;
import org.apache.camel.k.loader.yaml.spi.StepParser;
import org.apache.camel.k.support.RouteBuilders;

@Loader("yaml")
public class YamlSourceLoader implements SourceLoader {
    public static final ObjectMapper MAPPER;

    static {
        // register custom reifiers auto-generated from the step parser definitions
        YamlReifiers.registerReifiers();

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
    public List<String> getSupportedLanguages() {
        return Collections.singletonList("yaml");
    }

    @Override
    public RoutesBuilder load(Runtime runtime, Source source) {
        return RouteBuilders.route(source, (reader, builder) -> {
            final StepParser.Resolver resolver = StepParser.Resolver.caching(new YamlStepResolver());

            for (Step step : MAPPER.readValue(reader, Step[].class)) {
                StartStepParser.invoke(
                    new StepParser.Context(builder, null, MAPPER, step.node, resolver),
                    step.id);
            }
        });
    }
}

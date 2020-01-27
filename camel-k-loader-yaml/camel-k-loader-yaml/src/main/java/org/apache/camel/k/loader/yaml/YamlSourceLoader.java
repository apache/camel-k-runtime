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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.k.Runtime;
import org.apache.camel.k.Source;
import org.apache.camel.k.SourceLoader;
import org.apache.camel.k.annotation.Loader;
import org.apache.camel.k.loader.yaml.model.Step;
import org.apache.camel.k.loader.yaml.parser.StartStepParser;
import org.apache.camel.k.loader.yaml.parser.StepParser;
import org.apache.camel.model.Block;
import org.apache.camel.model.OptionalIdentifiedDefinition;
import org.apache.camel.model.OtherAttributesAware;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.RoutesDefinition;
import org.apache.camel.model.rest.RestDefinition;
import org.apache.camel.model.rest.RestsDefinition;


@Loader("yaml")
public class YamlSourceLoader implements SourceLoader {
    static {
        YamlReifiers.registerReifiers();
    }

    private final ObjectMapper mapper;

    public YamlSourceLoader() {
        YAMLFactory yamlFactory = new YAMLFactory()
            .configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, true)
            .configure(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID, false);

        this.mapper = new ObjectMapper(yamlFactory)
            .registerModule(new YamlModule())
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
            .setPropertyNamingStrategy(PropertyNamingStrategy.KEBAB_CASE)
            .enable(MapperFeature.USE_GETTERS_AS_SETTERS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(SerializationFeature.INDENT_OUTPUT);

        mapper.addMixIn(ProcessorDefinition.class, ProcessorDefinitionMixIn.class);
    }

    @Override
    public List<String> getSupportedLanguages() {
        return Collections.singletonList("yaml");
    }

    @Override
    public Result load(Runtime runtime, Source source) throws Exception {
        return Result.on(
            builder(source.resolveAsInputStream(runtime.getCamelContext()))
        );
    }

    final ObjectMapper mapper() {
        return mapper;
    }

    final RouteBuilder builder(InputStream is) {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                final CamelContext camelContext = getContext();
                final List<RouteDefinition> routes = new ArrayList<>();
                final List<RestDefinition> rests = new ArrayList<>();

                try {
                    for (Step step : mapper.readValue(is, Step[].class)) {
                        final StepParser.Context context = new StepParser.Context(camelContext, mapper, step.node);
                        final ProcessorDefinition<?> root = StartStepParser.invoke(context, step.id);

                        if (root == null) {
                            throw new IllegalStateException("No route definition");
                        }
                        if (!(root instanceof RouteDefinition)) {
                            throw new IllegalStateException("Root definition should be of type RouteDefinition");
                        }

                        RouteDefinition r = (RouteDefinition) root;
                        if (r.getRestDefinition() == null) {
                            routes.add(r);
                        } else {
                            rests.add(r.getRestDefinition());
                        }
                    }

                    if (!routes.isEmpty()) {
                        RoutesDefinition definition = new RoutesDefinition();
                        definition.setRoutes(routes);

                        setRouteCollection(definition);
                    }
                    if (!rests.isEmpty()) {
                        RestsDefinition definition = new RestsDefinition();
                        definition.setRests(rests);

                        setRestCollection(definition);
                    }
                } finally {
                    is.close();
                }
            }
        };
    }

    /**
     * ProcessorDefinition declares multiple methods for setBody and Jackson get confused
     * about what method to use so to hide such fields from the deserialization process
     * without having to change the original class, a MixIn is required.
     */
    public abstract static class ProcessorDefinitionMixIn<Type extends ProcessorDefinition<Type>>
        extends OptionalIdentifiedDefinition<Type>
        implements Block, OtherAttributesAware {

        @JsonIgnore
        public abstract <Result> Type setBody(Supplier<Result> supplier);

        @JsonIgnore
        public abstract <Result> Type setBody(Function<Exchange, Result> function);
    }
}

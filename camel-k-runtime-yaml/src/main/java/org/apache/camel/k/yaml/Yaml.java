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
package org.apache.camel.k.yaml;

import java.util.function.Function;
import java.util.function.Supplier;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.apache.camel.Exchange;
import org.apache.camel.k.yaml.model.Definitions;
import org.apache.camel.model.Block;
import org.apache.camel.model.OptionalIdentifiedDefinition;
import org.apache.camel.model.OtherAttributesAware;
import org.apache.camel.model.ProcessorDefinition;

public final class Yaml {
    public static final ObjectMapper MAPPER = mapper();

    public static ObjectMapper mapper() {
        YAMLFactory yamlFactory = new YAMLFactory()
            .configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, true)
            .configure(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID, false);

        ObjectMapper mapper = new ObjectMapper(yamlFactory)
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
            .setPropertyNamingStrategy(PropertyNamingStrategy.KEBAB_CASE)
            .enable(MapperFeature.USE_GETTERS_AS_SETTERS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(SerializationFeature.INDENT_OUTPUT);

        mapper.addMixIn(ProcessorDefinition.class, ProcessorDefinitionMixIn.class);

        Definitions.dataFormatDefinition().forEach(
            (id, type) -> mapper.registerSubtypes(new NamedType(type, id))
        );
        Definitions.expressionDefinition().forEach(
            (id, type) -> mapper.registerSubtypes(new NamedType(type, id))
        );

        return mapper;
    }

    public static JsonNode node(String name, JsonNode value) {
        return Yaml.MAPPER.createObjectNode().set(name, value);
    }

    /**
     * ProcessorDefinition declares multiple methods for setBody and Jackson get confused
     * about what method to use so to hide such fields from the deserialization process
     * without having to change the original class, a MixIn is required.
     */
    public abstract class ProcessorDefinitionMixIn<Type extends ProcessorDefinition<Type>>
        extends OptionalIdentifiedDefinition<Type>
        implements Block, OtherAttributesAware {

        @JsonIgnore
        public abstract <Result> Type setBody(Supplier<Result> supplier);

        @JsonIgnore
        public abstract <Result> Type setBody(Function<Exchange, Result> function);
    }

}

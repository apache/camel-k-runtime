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
package org.apache.camel.k.loader.yaml.parser;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import org.apache.camel.k.annotation.yaml.YAMLNodeDefinition;
import org.apache.camel.k.annotation.yaml.YAMLStepParser;
import org.apache.camel.k.loader.yaml.model.Step;
import org.apache.camel.k.loader.yaml.spi.StartStepParser;
import org.apache.camel.k.loader.yaml.support.StepParserSupport;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.model.rest.RestDefinition;
import org.apache.camel.util.ObjectHelper;

@YAMLStepParser(id = "rest", definition = RestStepParser.Definition.class)
public class RestStepParser implements StartStepParser {
    @Override
    public Object process(Context context) {
        Definition definition = context.node(Definition.class);

        StepParserSupport.notNull(definition.uri, "uri");
        StepParserSupport.notNull(definition.verb, "verb");
        StepParserSupport.notNull(definition.steps, "steps");

        RestDefinition rest = context.builder().rest().verb(definition.verb, definition.uri);

        ObjectHelper.ifNotEmpty(definition.apiDocs, rest::apiDocs);
        ObjectHelper.ifNotEmpty(definition.enableCORS, rest::enableCORS);
        ObjectHelper.ifNotEmpty(definition.consumes, rest::consumes);
        ObjectHelper.ifNotEmpty(definition.produces, rest::produces);
        ObjectHelper.ifNotEmpty(definition.bindingMode, rest::bindingMode);
        ObjectHelper.ifNotEmpty(definition.type, rest::type);
        ObjectHelper.ifNotEmpty(definition.outType, rest::outType);
        ObjectHelper.ifNotEmpty(definition.id, rest::id);
        ObjectHelper.ifNotEmpty(definition.description, rest::description);

        return StepParserSupport.convertSteps(
            context,
            rest.route(),
            definition.steps
        );
    }

    @YAMLNodeDefinition
    public static final class Definition {
        @JsonProperty
        public String id;

        @JsonProperty
        public String description;

        @JsonProperty
        public String verb;

        @JsonProperty
        public String uri;

        @JsonProperty
        public String consumes;

        @JsonProperty
        public String produces;

        @JsonProperty("binding-mode")
        @JsonSetter(nulls = Nulls.SKIP)
        public RestBindingMode bindingMode = RestBindingMode.auto;

        @JsonProperty
        public Boolean enableCORS;

        @JsonProperty
        public Boolean apiDocs;

        @JsonProperty
        public List<Step> steps;

        @JsonProperty
        public Class<?> type;

        @JsonProperty
        public Class<?> outType;
    }
}


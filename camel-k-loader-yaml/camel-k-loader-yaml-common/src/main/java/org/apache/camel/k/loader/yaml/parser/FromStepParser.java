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
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.camel.k.annotation.yaml.YAMLNodeDefinition;
import org.apache.camel.k.annotation.yaml.YAMLStepParser;
import org.apache.camel.k.loader.yaml.model.Step;
import org.apache.camel.k.loader.yaml.spi.StartStepParser;
import org.apache.camel.k.loader.yaml.support.StepParserSupport;

@YAMLStepParser(id = "from", definition = FromStepParser.Definition.class)
public class FromStepParser implements StartStepParser {
    @Override
    public Object process(Context context) {
        final Definition definition = context.node(Definition.class);
        if (definition.uri == null && definition.scheme == null) {
            throw new IllegalArgumentException("Either uri or scheme must be set");
        }

        String uri = StepParserSupport.createEndpointUri(
            context.getCamelContext(),
            definition.uri != null ? definition.uri : definition.scheme,
            definition.parameters);

        // as this is a start converter, steps are mandatory
        StepParserSupport.notNull(definition.steps, "steps");

        return StepParserSupport.convertSteps(
            context,
            context.builder().from(uri),
            definition.steps
        );
    }

    @YAMLNodeDefinition
    public static final class Definition implements HasEndpointConsumer {
        public String scheme;
        public String uri;
        public Map<String, Object> parameters;

        @JsonProperty(required = true)
        public List<Step> steps;

        @JsonIgnore
        @Override
        public void setEndpointScheme(String scheme) {
            this.scheme = scheme;
        }

        @JsonIgnore
        @Override
        public String getEndpointScheme() {
            return this.scheme ;
        }

        @JsonProperty(required = true)
        @Override
        public void setUri(String uri) {
            this.uri = uri;
        }

        @JsonProperty
        @Override
        public String getUri() {
            return this.uri;
        }

        @JsonProperty
        @Override
        public void setParameters(Map<String, Object> parameters) {
            this.parameters = parameters;
        }

        @JsonProperty
        @Override
        public Map<String, Object> getParameters() {
            return this.parameters;
        }
    }
}


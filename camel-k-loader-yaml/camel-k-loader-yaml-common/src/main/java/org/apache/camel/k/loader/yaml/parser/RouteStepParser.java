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

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.camel.k.annotation.yaml.YAMLNodeDefinition;
import org.apache.camel.k.annotation.yaml.YAMLStepParser;
import org.apache.camel.k.loader.yaml.model.Step;
import org.apache.camel.k.loader.yaml.spi.StartStepParser;
import org.apache.camel.k.loader.yaml.spi.StepParserSupport;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.URISupport;

@YAMLStepParser(id = "route", definition = RouteStepParser.Definition.class)
public class RouteStepParser implements StartStepParser {
    @Override
    public Object process(Context context) {
        final Definition definition = context.node(Definition.class);
        final String uri = definition.from.getEndpointUri();
        final RouteDefinition route = context.builder().from(uri);

        ObjectHelper.ifNotEmpty(definition.id, route::routeId);
        ObjectHelper.ifNotEmpty(definition.group, route::routeGroup);

        // as this is a start converter, steps are mandatory
        StepParserSupport.notNull(definition.steps, "steps");

        return StepParserSupport.convertSteps(
            context,
            route,
            definition.steps
        );
    }

    @YAMLNodeDefinition
    public static final class Definition {
        @JsonProperty
        public String id;
        @JsonProperty
        public String group;
        @JsonProperty(required = true)
        public From from;
        @JsonProperty(required = true)
        public List<Step> steps;
    }

    @YAMLNodeDefinition
    public static final class From {
        @JsonProperty
        public String uri;
        @JsonProperty
        public Map<String, Object> parameters;

        public From() {
        }

        public From(String uri) {
            this.uri = uri;
        }

        @JsonIgnore
        public String getEndpointUri() {
            String answer = uri;

            if (parameters != null) {
                try {
                    answer = URISupport.appendParametersToURI(answer, parameters);
                } catch (URISyntaxException | UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }

            return answer;
        }
    }
}


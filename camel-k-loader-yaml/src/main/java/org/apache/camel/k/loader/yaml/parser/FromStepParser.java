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
import org.apache.camel.k.annotation.yaml.YAMLStepParser;
import org.apache.camel.k.loader.yaml.model.Step;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.util.URISupport;

@YAMLStepParser("from")
public class FromStepParser implements StartStepParser {
    @Override
    public ProcessorDefinition<?> toStartProcessor(Context context) {
        final FromStepDefinition definition = context.node(FromStepDefinition.class);
        final String uri = definition.getEndpointUri();
        final RouteDefinition route = new RouteDefinition().from(uri);

        // as this is a start converter, steps are mandatory
        StepParserSupport.notNull(definition.steps, "steps");

        return StepParserSupport.convertSteps(
            context,
            route,
            definition.steps
        );
    }

    public static final class FromStepDefinition implements Step.Definition {
        public String uri;
        public Map<String, Object> parameters;
        public List<Step> steps;

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


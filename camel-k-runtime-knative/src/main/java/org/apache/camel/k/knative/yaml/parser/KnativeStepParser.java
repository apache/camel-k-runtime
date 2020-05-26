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
package org.apache.camel.k.knative.yaml.parser;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.camel.component.knative.spi.Knative;
import org.apache.camel.k.loader.yaml.spi.ProcessorStepParser;
import org.apache.camel.k.loader.yaml.spi.StartStepParser;
import org.apache.camel.k.loader.yaml.spi.StepParserSupport;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.ToDefinition;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.URISupport;

public class KnativeStepParser implements ProcessorStepParser, StartStepParser {
    @Override
    public ProcessorDefinition<?> toStartProcessor(Context context) {
        final Definition definition = context.node(Definition.class);
        final String uri = definition.getEndpointUri();
        final RouteDefinition route = context.builder().from(uri);

        // steps are mandatory
        ObjectHelper.notNull(definition.steps, "from steps");

        return StepParserSupport.convertSteps(
            context,
            route,
            definition.steps
        );
    }

    @Override
    public ProcessorDefinition<?> toProcessor(Context context) {
        return new ToDefinition(context.node(Definition.class).getEndpointUri());
    }

    public static final class Definition {
        public Knative.Type type;
        public String name;
        public Map<String, Object> parameters;
        public List<org.apache.camel.k.loader.yaml.model.Step> steps;

        @JsonIgnore
        public String getEndpointUri() {
            String answer = String.format("knative:%s/%s", type.name(), name);

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

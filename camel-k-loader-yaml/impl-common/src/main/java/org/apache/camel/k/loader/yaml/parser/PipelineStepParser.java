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
import org.apache.camel.k.annotation.yaml.YAMLNodeDefinition;
import org.apache.camel.k.annotation.yaml.YAMLStepParser;
import org.apache.camel.k.loader.yaml.model.Step;
import org.apache.camel.k.loader.yaml.spi.ProcessorStepParser;
import org.apache.camel.k.loader.yaml.support.StepParserSupport;
import org.apache.camel.model.PipelineDefinition;
import org.apache.camel.model.ProcessorDefinition;

@YAMLStepParser(id = "pipeline", definition = PipelineStepParser.Definition.class)
public class PipelineStepParser implements ProcessorStepParser {
    @Override
    public ProcessorDefinition<?> toProcessor(Context context) {
        final Definition definition = context.node(Definition.class);
        final PipelineDefinition answer = new PipelineDefinition();

        StepParserSupport.notNull(definition.steps, "steps");

        return StepParserSupport.convertSteps(
            context,
            answer,
            definition.steps
        );
    }

    @YAMLNodeDefinition
    public static final class Definition {
        @JsonProperty
        public List<Step> steps;
    }
}


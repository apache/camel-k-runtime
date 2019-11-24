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

import org.apache.camel.k.annotation.yaml.YAMLStepParser;
import org.apache.camel.k.loader.yaml.model.Step;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.SamplingDefinition;
import org.apache.camel.reifier.ProcessorReifier;
import org.apache.camel.reifier.SamplingReifier;

@YAMLStepParser("sample")
public class SampleStepParser implements ProcessorStepParser {
    static {
        ProcessorReifier.registerReifier(Definition.class, SamplingReifier::new);
    }

    @Override
    public ProcessorDefinition<?> toProcessor(Context context) {
        Definition definition = context.node(Definition.class);

        return StepParserSupport.convertSteps(
            context,
            definition,
            definition.steps
        );
    }

    public static final class Definition extends SamplingDefinition implements Step.Definition {
        public List<Step> steps;
    }
}


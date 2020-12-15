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

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.camel.k.annotation.yaml.YAMLElement;
import org.apache.camel.k.annotation.yaml.YAMLNodeDefinition;
import org.apache.camel.k.annotation.yaml.YAMLStepParser;
import org.apache.camel.k.loader.yaml.model.Step;
import org.apache.camel.k.loader.yaml.spi.ProcessorStepParser;
import org.apache.camel.k.loader.yaml.support.StepParserSupport;
import org.apache.camel.model.CatchDefinition;
import org.apache.camel.model.FinallyDefinition;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.TryDefinition;
import org.apache.camel.model.WhenDefinition;
import org.apache.camel.reifier.CatchReifier;
import org.apache.camel.reifier.FinallyReifier;
import org.apache.camel.reifier.TryReifier;

@YAMLStepParser(id = "do-try", definition = DoTryStepParser.DoTryDefinition.class)
public class DoTryStepParser implements ProcessorStepParser {
    @Override
    public ProcessorDefinition<?> toProcessor(Context context) {
        DoTryDefinition definition = context.node(DoTryDefinition.class);

        ProcessorDefinition<?> processor = StepParserSupport.convertSteps(
            context,
            definition.delegate,
            definition.steps
        );

        if (definition.doCatch != null) {
            StepParserSupport.convertSteps(
                    context,
                    definition.doCatch,
                    definition.doCatch.steps
            );

            CatchDefinition cd = new CatchDefinition();
            cd.setExceptions(definition.doCatch.getExceptions());
            cd.setOutputs(definition.doCatch.getOutputs());
            // doCatch.when
            if (definition.doCatch.when != null) {
                StepParserSupport.convertSteps(
                        context,
                        cd.onWhen(definition.doCatch.when.getExpression()),
                        definition.doCatch.when.steps
                );
                cd.setOnWhen(definition.doCatch.when);
            }

            processor.addOutput(cd);
        }

        if (definition.doFinally != null) {
            StepParserSupport.convertSteps(
                    context,
                    definition.doFinally,
                    definition.doFinally.steps
            );

            FinallyDefinition fd = new FinallyDefinition();
            fd.setOutputs(definition.doFinally.getOutputs());
            processor.addOutput(fd);
        }

        return processor;
    }

    @YAMLNodeDefinition(reifiers = TryReifier.class)
    public static final class DoTryDefinition {
        public TryDefinition delegate = new TryDefinition();
        @JsonProperty
        public List<Step> steps;
        @YAMLElement
        @JsonAlias("do-catch")
        public DoCatchDefinition doCatch;
        @YAMLElement
        @JsonAlias("do-finally")
        public DoFinallyDefinition doFinally;
    }

    @YAMLNodeDefinition(reifiers = CatchReifier.class)
    public static final class DoCatchDefinition extends CatchDefinition {
        @YAMLElement
        @JsonAlias("do-when")
        public When when;
        @JsonProperty
        public List<Step> steps;

        public static final class When extends WhenDefinition implements HasExpression {
            @JsonProperty
            public List<Step> steps;
        }
    }

    @YAMLNodeDefinition(reifiers = FinallyReifier.class)
    public static final class DoFinallyDefinition extends FinallyDefinition {
        @JsonProperty
        public List<Step> steps;
    }
}


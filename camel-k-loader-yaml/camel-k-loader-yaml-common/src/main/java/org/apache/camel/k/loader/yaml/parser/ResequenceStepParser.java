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
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.camel.Expression;
import org.apache.camel.k.annotation.yaml.YAMLNodeDefinition;
import org.apache.camel.k.annotation.yaml.YAMLStepParser;
import org.apache.camel.k.loader.yaml.model.Step;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.ResequenceDefinition;
import org.apache.camel.model.config.BatchResequencerConfig;
import org.apache.camel.model.config.StreamResequencerConfig;
import org.apache.camel.reifier.ResequenceReifier;

@YAMLStepParser("resequence")
public class ResequenceStepParser implements ProcessorStepParser {
    @Override
    public ProcessorDefinition<?> toProcessor(Context context) {
        Definition definition = context.node(Definition.class);

        return StepParserSupport.convertSteps(
            context,
            definition,
            definition.steps
        );
    }

    @YAMLNodeDefinition(reifiers = ResequenceReifier.class)
    public static final class Definition extends ResequenceDefinition implements HasExpression {
        public List<Step> steps;

        @JsonIgnore
        public void setExpression(Expression expression) {
            super.setExpression(expression);
        }

        @JsonAlias("batch-config")
        public void setBatchConfig(BatchResequencerConfig config) {
            if (getResequencerConfig() != null) {
                throw new IllegalArgumentException("And resequencer config has already been set");
            }
            setResequencerConfig(config);
        }

        @JsonAlias("stream-config")
        public void setStreamConfig(StreamResequencerConfig config) {
            if (getResequencerConfig() != null) {
                throw new IllegalArgumentException("And resequencer config has already been set");
            }
            setResequencerConfig(config);
        }
    }
}


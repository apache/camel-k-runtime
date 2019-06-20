/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.k.yaml.parser;

import java.util.List;

import org.apache.camel.model.ChoiceDefinition;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.WhenDefinition;

public class ChoiceStepParser implements ProcessorStepParser {
    @Override
    public ProcessorDefinition<?> toProcessor(Context context) {

        final Definition definition = context.node(Definition.class);
        final ChoiceDefinition choice = new ChoiceDefinition();

        StepParserSupport.notNull(definition.when,"when");

        for (Definition.When whenDefinition : definition.when) {
            StepParserSupport.notNull(whenDefinition.getExpression(), "when.expression");
            StepParserSupport.notNull(whenDefinition.steps, "when.steps");

            StepParserSupport.convertSteps(
                context,
                choice.when().expression(whenDefinition.getExpression()),
                whenDefinition.steps
            );
        }

        if (definition.otherwise != null) {
            StepParserSupport.notNull(definition.otherwise.steps, "otherwise.steps");

            StepParserSupport.convertSteps(
                context,
                choice.otherwise(),
                definition.otherwise.steps);
        }

        return choice;
    }

    public static final class Definition {
        public List<When> when;
        public Otherwise otherwise;

        public static final class When extends WhenDefinition implements HasExpression {
            public List<org.apache.camel.k.yaml.model.Step> steps;
        }

        public static final class Otherwise {
            public List<org.apache.camel.k.yaml.model.Step> steps;
        }
    }
}


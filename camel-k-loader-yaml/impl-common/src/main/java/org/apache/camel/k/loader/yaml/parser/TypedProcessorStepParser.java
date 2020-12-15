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

import java.io.IOException;

import org.apache.camel.k.loader.yaml.model.Step;
import org.apache.camel.k.loader.yaml.spi.ProcessorStepParser;
import org.apache.camel.k.loader.yaml.support.StepParserSupport;
import org.apache.camel.model.ExpressionNode;
import org.apache.camel.model.OutputNode;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.language.ExpressionDefinition;
import org.apache.camel.util.ObjectHelper;

@SuppressWarnings("unchecked")
public class TypedProcessorStepParser implements ProcessorStepParser {
    private final Class<? extends ProcessorDefinition> type;
    private final boolean hasOutputs;
    private final boolean hasExpressions;

    public TypedProcessorStepParser(Class<? extends ProcessorDefinition> type) {
        this.type = ObjectHelper.notNull(type, "type");
        this.hasOutputs = OutputNode.class.isAssignableFrom(type);
        this.hasExpressions = ExpressionNode.class.isAssignableFrom(type);
    }

    @Override
    public ProcessorDefinition<?> toProcessor(Context context) {
        var def = unwrap(context);

        if (this.hasExpressions) {
            setExpression(context, (ExpressionNode)def);
        }

        return this.hasOutputs
            ? context.node(Step.STEPS_NODE_ID, Step.STEPS_TYPE)
                .map(steps -> StepParserSupport.convertSteps(context, def, steps))
                .orElse(def)
            : def;
    }

    /**
     * Unwrap the Json payload to an instance of the give type.
     *
     * @param context the {@link org.apache.camel.k.loader.yaml.spi.StepParser.Context}
     * @return a {@link ProcessorDefinition} instance.
     */
    protected ProcessorDefinition unwrap(Context context) {
        return context.node(type);
    }

    /**
     * Unwrap the Json paylod to an {@link ExpressionDefinition} instance and set it ot the target.
     *
     * @param context the {@link org.apache.camel.k.loader.yaml.spi.StepParser.Context}
     * @param target the node supporting expression.
     */
    protected void setExpression(Context context, ExpressionNode target) {
        try {
            ExpressionDefinition ed = HasExpression.getExpressionType(context.mapper(), context.node());
            if (ed != null) {
                target.setExpression(ed);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}

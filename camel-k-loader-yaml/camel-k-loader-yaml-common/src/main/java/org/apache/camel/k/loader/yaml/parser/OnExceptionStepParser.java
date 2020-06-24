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
import org.apache.camel.k.annotation.yaml.YAMLNodeDefinition;
import org.apache.camel.k.annotation.yaml.YAMLStepParser;
import org.apache.camel.k.loader.yaml.model.Step;
import org.apache.camel.k.loader.yaml.spi.ProcessorStepParser;
import org.apache.camel.k.loader.yaml.spi.StartStepParser;
import org.apache.camel.k.loader.yaml.spi.StepParserSupport;
import org.apache.camel.model.ExpressionSubElementDefinition;
import org.apache.camel.model.OnExceptionDefinition;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.RedeliveryPolicyDefinition;
import org.apache.camel.model.WhenDefinition;
import org.apache.camel.model.language.ConstantExpression;
import org.apache.camel.model.language.ExpressionDefinition;
import org.apache.camel.reifier.OnExceptionReifier;

import static org.apache.camel.util.ObjectHelper.ifNotEmpty;

@YAMLStepParser(id = "on-exception", definitions = OnExceptionStepParser.Definition.class)
public class OnExceptionStepParser implements StartStepParser, ProcessorStepParser {
    @SuppressWarnings("unchecked")
    @Override
    public ProcessorDefinition<?> toStartProcessor(Context context) {
        final Definition definition = context.node(Definition.class);
        final OnExceptionDefinition onException = context.builder().onException();

        if (definition.exceptions == null) {
            definition.exceptions = List.of(Exception.class.getName());
        }

        onException.setExceptions(definition.exceptions);
        onException.setRouteScoped(false);

        mapToOnException(context, definition, onException);

        return StepParserSupport.convertSteps(
            context,
            onException,
            definition.steps);
    }

    @Override
    public ProcessorDefinition<?> toProcessor(Context context) {
        final Definition definition = context.node(Definition.class);
        final OnExceptionDefinition onException = new OnExceptionDefinition();

        if (definition.exceptions == null) {
            definition.exceptions = List.of(Exception.class.getName());
        }

        onException.setExceptions(definition.exceptions);
        onException.setRouteScoped(true);

        mapToOnException(context, definition, onException);

        return StepParserSupport.convertSteps(
            context,
            onException,
            definition.steps);
    }

    private static void mapToOnException(Context context, Definition definition, OnExceptionDefinition onException) {
        ifNotEmpty(definition.retryWhile, onException::setRetryWhile);
        ifNotEmpty(definition.handled, onException::setHandled);
        ifNotEmpty(definition.continued, onException::setContinued);
        ifNotEmpty(definition.continued, onException::setContinued);
        ifNotEmpty(definition.redeliveryPolicyType, onException::setRedeliveryPolicyType);
        ifNotEmpty(definition.redeliveryPolicyRef, onException::setRedeliveryPolicyRef);
        ifNotEmpty(definition.onRedeliveryRef, onException::setOnRedeliveryRef);
        ifNotEmpty(definition.onExceptionOccurredRef, onException::setOnExceptionOccurredRef);
        ifNotEmpty(definition.useOriginalMessage, val -> onException.setUseOriginalMessage(Boolean.toString(val)));
        ifNotEmpty(definition.useOriginalBody, val -> onException.setUseOriginalBody(Boolean.toString(val)));

        if (definition.onWhen != null) {
            StepParserSupport.notNull(definition.onWhen.steps, "onWhen.steps");

            StepParserSupport.convertSteps(
                context,
                definition.onWhen,
                definition.onWhen.steps
            );

            onException.setOnWhen(definition.onWhen);
        }
    }

    @YAMLNodeDefinition(reifiers = OnExceptionReifier.class)
    public static final class Definition {
        @JsonProperty
        public List<Step> steps;

        @JsonAlias("exceptions")
        public List<String> exceptions;

        @JsonAlias("when")
        public When onWhen;
        @JsonAlias("retry-while")
        public ExpressionElement retryWhile;
        @JsonAlias("handled")
        public MaybeBooleanExpressionElement handled;
        @JsonAlias("continued")
        public MaybeBooleanExpressionElement continued;

        @JsonAlias("redelivery-policy")
        public RedeliveryPolicyDefinition redeliveryPolicyType;
        @JsonAlias("redelivery-policy-ref")
        public String redeliveryPolicyRef;

        @JsonAlias("on-redelivery-ref")
        public String onRedeliveryRef;
        @JsonAlias("on-exception-occurred-ref")
        public String onExceptionOccurredRef;
        @JsonAlias("use-original-message")
        public boolean useOriginalMessage;
        @JsonAlias("use-original-body")
        public boolean useOriginalBody;

        @YAMLNodeDefinition
        public static final class When extends WhenDefinition implements HasExpression {
            @JsonProperty
            public List<Step> steps;
        }

        @YAMLNodeDefinition
        public static final class ExpressionElement extends ExpressionSubElementDefinition implements HasExpression {
            @Override
            public void setExpression(ExpressionDefinition expressionDefinition) {
                super.setExpressionType(expressionDefinition);
            }

            @Override
            public ExpressionDefinition getExpression() {
                return super.getExpressionType();
            }
        }

        @YAMLNodeDefinition
        public static final class MaybeBooleanExpressionElement extends ExpressionSubElementDefinition implements HasExpression {
            public MaybeBooleanExpressionElement() {
            }

            public MaybeBooleanExpressionElement(boolean argument) {
                setExpression(new ConstantExpression(Boolean.toString(argument)));
            }

            @Override
            public void setExpression(ExpressionDefinition expressionDefinition) {
                super.setExpressionType(expressionDefinition);
            }

            @Override
            public ExpressionDefinition getExpression() {
                return super.getExpressionType();
            }
        }
    }
}


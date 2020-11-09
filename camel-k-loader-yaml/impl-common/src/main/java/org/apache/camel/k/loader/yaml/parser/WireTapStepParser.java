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
import java.util.Map;

import org.apache.camel.k.annotation.yaml.YAMLNodeDefinition;
import org.apache.camel.k.annotation.yaml.YAMLStepParser;
import org.apache.camel.k.loader.yaml.spi.ProcessorStepParser;
import org.apache.camel.k.loader.yaml.support.StepParserSupport;
import org.apache.camel.model.ExpressionSubElementDefinition;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.SetHeaderDefinition;
import org.apache.camel.model.ToDynamicDefinition;
import org.apache.camel.model.WireTapDefinition;
import org.apache.camel.model.language.ExpressionDefinition;
import org.apache.camel.reifier.WireTapReifier;
import org.apache.camel.util.ObjectHelper;

@YAMLStepParser(id = "wiretap", definition = WireTapStepParser.Definition.class)
public class WireTapStepParser implements ProcessorStepParser {
    @Override
    public ProcessorDefinition<?> toProcessor(Context context) {
        Definition definition = context.node(Definition.class);

        WireTapDefinition answer = new WireTapDefinition();
        ObjectHelper.ifNotEmpty(definition.processorRef, answer::setNewExchangeProcessorRef);
        ObjectHelper.ifNotEmpty(definition.executorServiceRef, answer::setExecutorServiceRef);
        ObjectHelper.ifNotEmpty(definition.onPrepareRef, answer::onPrepareRef);
        ObjectHelper.ifNotEmpty(definition.copy, answer::copy);
        ObjectHelper.ifNotEmpty(definition.dynamicUri, answer::dynamicUri);

        if (definition.newExchange != null) {
            answer.setNewExchangeExpression(definition.newExchange);

            if (definition.newExchange.headers != null) {
                answer.setHeaders(definition.newExchange.headers);
            }
        }

        answer.setUri(
            StepParserSupport.createEndpointUri(context.getCamelContext(), definition.getUri(), definition.parameters)
        );

        return answer;
    }

    @YAMLNodeDefinition(reifiers = WireTapReifier.class)
    public static final class Definition extends ToDynamicDefinition {
        public String processorRef;
        public String executorServiceRef;
        public String onPrepareRef;
        public Boolean copy;
        public Boolean dynamicUri;
        public NewExchangeDefinition newExchange;
        public Map<String, Object> parameters;
    }

    @YAMLNodeDefinition
    public static final class NewExchangeDefinition extends ExpressionSubElementDefinition implements HasExpression {
        public List<HeaderDefinition> headers;

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
    public static final class HeaderDefinition extends SetHeaderDefinition implements HasExpression {
    }
}


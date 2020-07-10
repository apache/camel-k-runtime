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

import com.fasterxml.jackson.annotation.JsonAlias;
import org.apache.camel.builder.DeadLetterChannelBuilder;
import org.apache.camel.builder.DefaultErrorHandlerBuilder;
import org.apache.camel.builder.ErrorHandlerBuilder;
import org.apache.camel.builder.ErrorHandlerBuilderRef;
import org.apache.camel.builder.NoErrorHandlerBuilder;
import org.apache.camel.k.annotation.yaml.YAMLNodeDefinition;
import org.apache.camel.k.annotation.yaml.YAMLStepParser;
import org.apache.camel.k.loader.yaml.spi.ProcessorStepParser;
import org.apache.camel.k.loader.yaml.spi.StartStepParser;
import org.apache.camel.k.loader.yaml.spi.StepParserSupport;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.RouteDefinition;

@YAMLStepParser(id = "error-handler", definition = ErrorHandlerStepParser.Definition.class)
public class ErrorHandlerStepParser implements StartStepParser, ProcessorStepParser {
    @Override
    public Object process(Context context) {
        final Definition definition = context.node(Definition.class);

        StepParserSupport.notNull(definition.builder, "builder");

        context.builder().errorHandler(definition.builder);

        return context.processor();
    }

    @Override
    public ProcessorDefinition<?> toProcessor(Context context) {
        final Definition definition = context.node(Definition.class);

        StepParserSupport.notNull(context.processor(), "processor");
        StepParserSupport.notNull(definition.builder, "builder");

        return context.processor(RouteDefinition.class).errorHandler(definition.builder);
    }

    @YAMLNodeDefinition()
    public static final class Definition {
        public ErrorHandlerBuilder builder;

        @JsonAlias("default")
        public void setDefault(DefaultErrorHandlerBuilder builder) {
            setBuilder(builder);
        }

        @JsonAlias("dead-letter-channel")
        public void setDeadLetterChannel(DeadLetterChannelBuilder builder) {
            setBuilder(builder);
        }

        @JsonAlias("no-error-handler")
        public void setNoErrorHandler(NoErrorHandlerBuilder builder) {
            setBuilder(builder);
        }

        @JsonAlias("ref")
        public void setRefHandler(String ref) {
            setBuilder(new ErrorHandlerBuilderRef(ref));
        }

        private void setBuilder(ErrorHandlerBuilder builder) {
            if (this.builder != null) {
                throw new IllegalArgumentException("An ErrorHandler has already been set");
            }

            this.builder = builder;
        }
    }
}


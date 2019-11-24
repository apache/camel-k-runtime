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

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.camel.Expression;
import org.apache.camel.k.annotation.yaml.YAMLStepParser;
import org.apache.camel.k.loader.yaml.model.Step;
import org.apache.camel.model.EnrichDefinition;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.reifier.EnrichReifier;
import org.apache.camel.reifier.ProcessorReifier;

@YAMLStepParser("enrich")
public class EnrichStepParser implements ProcessorStepParser {
    static {
        ProcessorReifier.registerReifier(Definition.class, EnrichReifier::new);
    }

    @Override
    public ProcessorDefinition<?> toProcessor(Context context) {
        return context.node(Definition.class);
    }

    public static final class Definition extends EnrichDefinition implements HasExpression, Step.Definition {
        @JsonIgnore
        public void setExpression(Expression expression) {
            super.setExpression(expression);
        }

        public void setStrategyRef(String aggregationStrategyRef) {
            super.setAggregationStrategyRef(aggregationStrategyRef);
        }

        public String getStrategyMethodName() {
            return super.getAggregationStrategyRef();
        }
    }
}


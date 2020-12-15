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

import org.apache.camel.k.annotation.yaml.YAMLStepParser;
import org.apache.camel.k.loader.yaml.spi.ProcessorStepParser;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.cloud.ServiceCallDefinition;
import org.apache.camel.model.language.ExpressionDefinition;

@YAMLStepParser(id = "service-call", definition = ServiceCallDefinition.class)
public class ServiceCallStepParser implements ProcessorStepParser {
    @Override
    public ProcessorDefinition<?> toProcessor(Context context) {
        final ServiceCallDefinition answer;

        if (context.node().isTextual()) {
            answer = new ServiceCallDefinition();
            answer.setName(context.node().textValue());
        } else {
            answer = context.node(ServiceCallDefinition.class);

            try {
                ExpressionDefinition ed = HasExpression.getExpressionType(context.mapper(), context.node());
                if (ed != null) {
                    answer.setExpression(ed);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return answer;
    }
}


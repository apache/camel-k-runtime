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

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.camel.k.loader.yaml.spi.ProcessorStepParser;
import org.apache.camel.k.loader.yaml.support.StepParserSupport;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.ToDefinition;
import org.apache.camel.util.StringHelper;

public class EndpointStepParser implements ProcessorStepParser {
    private final String scheme;

    public EndpointStepParser(String scheme) {
        this.scheme = scheme;
    }

    @Override
    public ProcessorDefinition<?> toProcessor(Context context) {
        final ObjectNode node = context.node(ObjectNode.class);
        final Map<String, Object> parameters = new HashMap<>();

        node.fields().forEachRemaining(entry -> {
            parameters.put(
                StringHelper.dashToCamelCase(entry.getKey()),
                entry.getValue().asText()
            );
        });

        return new ToDefinition(
            StepParserSupport.createEndpointUri(context.getCamelContext(), this.scheme, parameters)
        );
    }
}


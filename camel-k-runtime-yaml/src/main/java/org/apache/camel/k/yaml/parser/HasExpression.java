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

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.camel.k.yaml.Yaml;
import org.apache.camel.k.yaml.model.Definitions;
import org.apache.camel.model.language.ExpressionDefinition;

public interface HasExpression {

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
    void setExpression(ExpressionDefinition definition);

    ExpressionDefinition getExpression();

    @JsonAnySetter
    default void handleUnknownField(String id, JsonNode node) {
        Class<? extends ExpressionDefinition> type = Definitions.EXPRESSIONS_MAP.get(id);

        if (type == null) {
            throw new IllegalArgumentException("unknown expression type: " + type);
        }

        if (getExpression() != null) {
            throw new IllegalArgumentException("And expression has been set");
        }

        try {
            setExpression(
                Yaml.MAPPER.reader().forType(type).readValue(node)
            );
        } catch (IOException e) {
            throw new IllegalArgumentException("unable to parse expression definition", e);
        }
    }
}

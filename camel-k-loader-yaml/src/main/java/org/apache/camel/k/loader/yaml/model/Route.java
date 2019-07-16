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
package org.apache.camel.k.loader.yaml.model;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;

public class Route {
    private String id;
    private String group;
    private Definition definition;

    public Optional<String> getId() {
        return Optional.ofNullable(id);
    }

    public void setId(String id) {
        this.id = id;
    }

    public Optional<String> getGroup() {
        return Optional.ofNullable(group);
    }

    public void setGroup(String group) {
        this.group = group;
    }

    @JsonIgnore
    public Definition getDefinition() {
        return definition;
    }

    @JsonIgnore
    public void setDefinition(Definition routeDefinition) {
        this.definition = routeDefinition;
    }

    @JsonIgnore
    public void setDefinition(String type, JsonNode data) {
        this.definition = new Definition(type, data);
    }

    @JsonAnySetter
    public void handleUnknownField(String id, JsonNode node) {
        if (definition != null) {
            throw new IllegalArgumentException("A definition is already set: " + definition.type);
        }
        setDefinition(id, node);
    }

    public static class Definition {
        private final String type;
        private final JsonNode data;

        public Definition(String type, JsonNode data) {
            this.type = type;
            this.data = data;
        }

        public String getType() {
            return type;
        }

        public JsonNode getData() {
            return data;
        }
    }
}

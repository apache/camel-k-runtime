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

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;

import org.apache.camel.k.annotation.yaml.YAMLStepParser;
import org.apache.camel.k.loader.yaml.model.Node;
import org.apache.camel.k.loader.yaml.model.Step;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.RouteDefinition;

@YAMLStepParser("route")
public class RouteStepParser implements StartStepParser {
    @Override
    public ProcessorDefinition<?> toStartProcessor(Context context) {
        final Definition definition = context.node(Definition.class);

        final ProcessorDefinition<?> root = StartStepParser.invoke(
            ProcessorStepParser.Context.of(context, definition.getRoot().getData()),
            definition.getRoot().getType());

        if (root == null) {
            throw new IllegalStateException("No route definition");
        }
        if (!(root instanceof RouteDefinition)) {
            throw new IllegalStateException("Root definition should be of type RouteDefinition");
        }

        definition.getId().ifPresent(root::routeId);
        definition.getGroup().ifPresent(root::routeGroup);

        return root;
    }

    public static final class Definition implements Step.Definition {
        private String id;
        private String group;
        private Node root;

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
        public Node getRoot() {
            return root;
        }

        @JsonIgnore
        public void setRoot(Node routeDefinition) {
            this.root = routeDefinition;
        }

        @JsonAnySetter
        public void handleUnknownField(String id, JsonNode node) {
            if (root != null) {
                throw new IllegalArgumentException("A root is already set: " + root.getType());
            }
            setRoot(new Node(id, node));
        }
    }

}


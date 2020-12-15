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
package org.apache.camel.k.loader.yaml.spi;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.CamelContext;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.spi.HasCamelContext;
import org.apache.camel.util.ObjectHelper;

public interface StepParser {
    String SERVICE_LOCATION = "META-INF/services/org/apache/camel/k/loader/yaml-parser/";

    /**
     * Context for step parsing.
     */
    class Context implements HasCamelContext {
        private final ObjectMapper mapper;
        private final RouteBuilder builder;
        private final ProcessorDefinition<?> processor;
        private final JsonNode node;
        private final Resolver resolver;

        public Context(RouteBuilder builder, ProcessorDefinition<?> processor, ObjectMapper mapper, JsonNode node, Resolver resolver) {
            this.builder = builder;
            this.processor = processor;
            this.mapper = mapper;
            this.node = node;
            this.resolver = ObjectHelper.notNull(resolver, "resolver");
        }

        @Override
        public CamelContext getCamelContext() {
            return builder.getContext();
        }

        public <T extends CamelContext> T getCamelContext(Class<T> type) {
            return builder.getContext().adapt(type);
        }

        public ProcessorDefinition<?> processor() {
            return this.processor;
        }

        public <T extends ProcessorDefinition<?>> T processor(Class<T> type) {
            return type.cast(this.processor);
        }

        public RouteBuilder builder() {
            return builder;
        }

        public JsonNode node() {
            return node;
        }

        public ObjectMapper mapper() {
            return this.mapper;
        }

        public <T> T node(Class<T> type) {
            ObjectHelper.notNull(node, "node");
            ObjectHelper.notNull(type, "type");

            final T definition;

            try {
                definition = mapper.reader().forType(type).readValue(node);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }

            if (definition == null) {
                throw new IllegalStateException("Unable to decode node " + node + " to type " + type);
            }

            return definition;
        }

        public <T> T node(TypeReference<T> type) {
            ObjectHelper.notNull(node, "node");
            ObjectHelper.notNull(type, "type");

            final T definition;

            try {
                definition = mapper.reader().forType(type).readValue(node);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }

            if (definition == null) {
                throw new IllegalStateException("Unable to decode node " + node + " to type " + type);
            }

            return definition;
        }

        public <T> Optional<T> node(String fieldName, Class<T> type) {
            ObjectHelper.notNull(node, "node");
            ObjectHelper.notNull(type, "type");

            JsonNode root = node.get(fieldName);
            if (root == null) {
                return Optional.empty();
            }

            final T definition;

            try {

                definition = mapper.reader().forType(type).readValue(root);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }

            return Optional.ofNullable(definition);
        }

        public <T> Optional<T> node(String fieldName, TypeReference<T> type) {
            ObjectHelper.notNull(node, "node");
            ObjectHelper.notNull(type, "type");

            JsonNode root = node.get(fieldName);
            if (root == null) {
                return Optional.empty();
            }

            final T definition;

            try {

                definition = mapper.reader().forType(type).readValue(root);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }

            return Optional.ofNullable(definition);
        }

        public <T extends StepParser> T lookup(Class<T> type, String stepId) {
            StepParser parser = lookup(stepId);
            if (type.isInstance(parser)) {
                return type.cast(parser);
            }

            throw new RuntimeException("No handler for step with id: " + stepId);
        }

        public StepParser lookup(String stepId) {
            StepParser parser = resolver.resolve(builder.getContext(), stepId);
            if (parser == null) {
                throw new RuntimeException("No handler for step with id: " + stepId);
            }

            return parser;
        }

        public static Context of(Context context, ProcessorDefinition<?> processor, JsonNode step) {
            return new Context(
                context.builder,
                processor,
                context.mapper,
                step,
                context.resolver
            );
        }

        public static Context of(Context context, ProcessorDefinition<?> processor) {
            return new Context(
                context.builder,
                processor,
                context.mapper,
                context.node,
                context.resolver
            );
        }

        public static Context of(Context context, JsonNode step) {
            return new Context(
                context.builder,
                context.processor,
                context.mapper,
                step,
                context.resolver
            );
        }
    }

    /**
     * Step resolver.
     */
    interface Resolver {
        StepParser resolve(CamelContext camelContext, String stepId);

        default StepParser lookup(CamelContext camelContext, String stepId) {
            StepParser answer = camelContext.getRegistry().lookupByNameAndType(stepId, StepParser.class);
            if (answer == null) {
                answer = camelContext.adapt(ExtendedCamelContext.class)
                    .getFactoryFinder(SERVICE_LOCATION)
                    .newInstance(stepId, StepParser.class)
                    .orElseThrow(() -> new RuntimeException("No handler for step with id: " + stepId));
            }

            return answer;
        }

        static Resolver caching(Resolver delegate) {
            final ConcurrentMap<String, StepParser> cache = new ConcurrentHashMap<>();
            return (camelContext, stepId) -> cache.computeIfAbsent(stepId, key -> delegate.resolve(camelContext, key));
        }
    }
}

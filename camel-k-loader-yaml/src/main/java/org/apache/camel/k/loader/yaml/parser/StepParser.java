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

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.CamelContext;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.NoFactoryAvailableException;
import org.apache.camel.util.ObjectHelper;

public interface StepParser {
    String SERVICE_LOCATION = "META-INF/services/org/apache/camel/k/loader/yaml-parser/";

    @SuppressWarnings("unchecked")
    static <T extends StepParser> T lookup(CamelContext camelContext, Class<T> type, String stepId) throws NoFactoryAvailableException {
        T converter = camelContext.getRegistry().lookupByNameAndType(stepId, type);
        if (converter == null) {
            converter = camelContext.adapt(ExtendedCamelContext.class)
                .getFactoryFinder(SERVICE_LOCATION)
                .newInstance(stepId, type)
                .orElseThrow(() -> new RuntimeException("No handler for step with id: " + stepId));
        }

        return converter;
    }

    class Context {
        private final ObjectMapper mapper;
        private final CamelContext camelContext;
        private final JsonNode node;

        public Context(CamelContext camelContext, ObjectMapper mapper, JsonNode node) {
            this.camelContext = camelContext;
            this.mapper = mapper;
            this.node = node;
        }

        public CamelContext camelContext() {
            return camelContext;
        }

        public <T extends CamelContext> T camelContext(Class<T> type) {
            return camelContext.adapt(type);
        }

        public JsonNode node() {
            return node;
        }

        public ObjectMapper mapper() {
            return mapper;
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

        public static Context of(Context context, JsonNode step) {
            return new Context(
                context.camelContext,
                context.mapper,
                step
            );
        }
    }

}

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
package org.apache.camel.k.loader.yaml.support.serde;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import org.apache.camel.CamelContext;
import org.apache.camel.k.loader.yaml.model.Step;
import org.apache.camel.k.loader.yaml.spi.TypeResolver;
import org.apache.camel.model.OutputNode;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.util.StringHelper;

public abstract class DeserializerSupport<T> extends StdDeserializer<T> {
    protected DeserializerSupport(Class<T> vc) {
        super(vc);
    }

    @Override
    public T deserialize(JsonParser parser, DeserializationContext context) throws IOException, JsonProcessingException {
        final T target;

        try {
            final JsonNode node = parser.getCodec().readTree(parser);
            if (node.getNodeType() == JsonNodeType.STRING) {
                target = handledTypeInstance(node.asText());
            } else {
                target = handledTypeInstance();
                setProperties(parser, target, node);

                if (target instanceof OutputNode) {
                    final CamelContext ctx = (CamelContext)context.getAttribute(CamelContext.class);
                    final TypeResolver tr = (TypeResolver)context.getAttribute(TypeResolver.class);
                    final ProcessorDefinition<?> def = (ProcessorDefinition<?>)target;
                    final JsonNode steps = node.get("steps");

                    if (steps != null) {
                        if (!steps.isArray()) {
                            throw new IllegalArgumentException("Steps should be an array");
                        }

                        for (Step step : parser.getCodec().treeToValue(steps, Step[].class)) {
                            Class<?> type = tr.lookup(ctx, step.id);
                            if (type == null) {
                                throw new IllegalArgumentException("Unable to determine type of step: " + step.id);
                            }
                            if (!ProcessorDefinition.class.isAssignableFrom(type)) {
                                throw new IllegalArgumentException("The resolved type is not of type ProcessorDefinition: " + type);
                            }

                            def.addOutput(
                                (ProcessorDefinition<?>)parser.getCodec().treeToValue(step.node, type)
                            );
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new DeserializerException(e);
        }

        return target;
    }

    protected void setProperties(JsonParser parser, T target, JsonNode node) throws Exception {
        final Iterator<Map.Entry<String, JsonNode>> it = node.fields();
        while (it.hasNext()) {
            final Map.Entry<String, JsonNode> entry = it.next();
            final String key = StringHelper.camelCaseToDash(entry.getKey()).toLowerCase(Locale.US);

            setProperty(parser, target, key, entry.getKey(), entry.getValue());
        }
    }

    /**
     * Set a property to the given target;
     *
     * @param parser the parser
     * @param target the target object
     * @param propertyKey the key
     * @param propertyName the name of the property
     * @param node the node holding the value
     */
    protected abstract void setProperty(JsonParser parser, T target, String propertyKey, String propertyName, JsonNode node) throws Exception;

    /**
     * Creates a new instance of the handled type.
     *
     * @return the instance.
     */
    protected abstract T handledTypeInstance();

    protected T handledTypeInstance(String value) {
        throw new UnsupportedOperationException("Cannot create an instance of type " +  handledType() + " from string");
    }

    // *********************************
    //
    // Conversion helpers
    //
    // *********************************

    protected static Class<?> asClass(String val) throws DeserializerException {
        try {
            return Class.forName(val);
        } catch (ClassNotFoundException e) {
            throw new DeserializerException("Unable to load class " + val, e);
        }
    }

    protected static Class<?>[] asClassArray(String val) throws DeserializerException {
        String[] vals = val.split(" ");
        Class<?>[] cls = new Class<?>[vals.length];
        for (int i = 0; i < vals.length; i++) {
            cls[i] = asClass(vals[i]);
        }
        return cls;
    }

    protected static byte[] asByteArray(String val) {
        return Base64.getDecoder().decode(val);
    }

    protected static List<String> asStringList(String val) {
        return List.of(val.split(" "));
    }

    protected static Set<String> asStringSet(String val) {
        return Set.of(val.split(" "));
    }

    protected static <T> List<T> asList(JsonParser parser, JsonNode node, Class<T> type) throws DeserializerException {
        if (!node.isArray()) {
            throw new UnsupportedOperationException("Unable to parse no array node");
        }

        List<T> answer = new ArrayList<>(node.size());

        Iterator<JsonNode> it = node.elements();
        while (it.hasNext()) {
            try {
                answer.add(parser.getCodec().treeToValue(it.next(), type));
            } catch (IOException e) {
                throw new DeserializerException("Unable to deserialize node " + node, e);
            }
        }

        return answer;
    }

    protected static <T> Set<T> asSet(JsonParser parser, JsonNode node, Class<T> type) throws DeserializerException {
        if (!node.isArray()) {
            throw new UnsupportedOperationException("Unable to parse no array node");
        }

        Set<T> answer = new HashSet<>();

        Iterator<JsonNode> it = node.elements();
        while (it.hasNext()) {
            try {
                answer.add(parser.getCodec().treeToValue(it.next(), type));
            } catch (IOException e) {
                throw new DeserializerException("Unable to deserialize node " + node, e);
            }
        }

        return answer;
    }

    protected static <T> T asType(JsonParser parser, JsonNode node, Class<T> type) throws DeserializerException {
        try {
            return parser.getCodec().treeToValue(node, type);
        } catch (IOException e) {
            throw new DeserializerException("Unable to deserialize node " + node, e);
        }
    }
}

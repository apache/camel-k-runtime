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
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.apache.camel.CamelContext;
import org.apache.camel.k.loader.yaml.support.StepParserSupport;
import org.apache.camel.util.StringHelper;

public abstract class EndpointDeserializerSupport<T> extends StdDeserializer<T> {
    private final String scheme;

    protected EndpointDeserializerSupport(Class<T> vc, String scheme) {
        super(vc);

        this.scheme = scheme;
    }

    @Override
    public T deserialize(JsonParser parser, DeserializationContext context) throws IOException, JsonProcessingException {
        final JsonNode node = parser.getCodec().readTree(parser);
        final CamelContext camelContext = (CamelContext) context.getAttribute(CamelContext.class);

        return createInstance(
            createEndpointUri(camelContext, node, this.scheme)
        );
    }

    /**
     * Creates a new instance of the handled type.
     *
     * @param uri the endpoint uri
     * @return the instance.
     */
    protected abstract T createInstance(String uri);

    // *************************************
    //
    // Helpers
    //
    // *************************************

    public static String createEndpointUri(CamelContext camelContext, JsonNode node, String scheme) {
        final Map<String, Object> parameters = new HashMap<>();

        node.fields().forEachRemaining(entry -> {
            parameters.put(
                StringHelper.dashToCamelCase(entry.getKey()),
                entry.getValue().asText()
            );
        });

        return StepParserSupport.createEndpointUri(camelContext, scheme, parameters);
    }
}

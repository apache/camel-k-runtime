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
package org.apache.camel.k.loader.yaml.deserializers;

import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.dsl.yaml.common.YamlSupport;
import org.apache.camel.dsl.yaml.deserializers.EndpointProducerDeserializersResolver;
import org.apache.camel.k.loader.yaml.YamlDeserializerEndpointAwareBase;
import org.apache.camel.model.ToDynamicDefinition;
import org.snakeyaml.engine.v2.nodes.Node;

public class ToDynamicDefinitionDeserializer extends YamlDeserializerEndpointAwareBase<ToDynamicDefinition> {
    public ToDynamicDefinitionDeserializer() {
        super(ToDynamicDefinition.class);
    }

    @Override
    protected ToDynamicDefinition newInstance() {
        return new ToDynamicDefinition();
    }

    @Override
    protected ToDynamicDefinition newInstance(String value) {
        return new ToDynamicDefinition(value);
    }

    @Override
    protected void setEndpointUri(CamelContext camelContext, ToDynamicDefinition target, Map<String, Object> parameters) {
        target.setUri(YamlSupport.createEndpointUri(camelContext, target.getUri(), parameters));
    }

    @Override
    protected boolean setProperty(ToDynamicDefinition target, String propertyKey, String propertyName, Node node) {
        switch(propertyKey) {
            case "allow-optimised-components": {
                String val = asText(node);
                target.setAllowOptimisedComponents(val);
                break;
            }
            case "auto-start-components": {
                String val = asText(node);
                target.setAutoStartComponents(val);
                break;
            }
            case "cache-size": {
                String val = asText(node);
                target.setCacheSize(val);
                break;
            }
            case "ignore-invalid-endpoint": {
                String val = asText(node);
                target.setIgnoreInvalidEndpoint(val);
                break;
            }
            case "inherit-error-handler": {
                String val = asText(node);
                target.setInheritErrorHandler(Boolean.valueOf(val));
                break;
            }
            case "pattern": {
                String val = asText(node);
                target.setPattern(val);
                break;
            }
            case "uri": {
                String val = asText(node);
                target.setUri(val);
                break;
            }
            default: {
                String uri = EndpointProducerDeserializersResolver.resolveEndpointUri(propertyKey, node);
                if (uri == null) {
                    return false;
                }
                if (target.getUri() != null) {
                    throw new IllegalStateException("url must not be set when using Endpoint DSL");
                }
                target.setUri(uri);
            }
        }
        return true;
    }
}

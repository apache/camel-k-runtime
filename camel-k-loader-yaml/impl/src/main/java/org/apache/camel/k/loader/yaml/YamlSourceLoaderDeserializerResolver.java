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
package org.apache.camel.k.loader.yaml;

import org.apache.camel.dsl.yaml.common.YamlDeserializerResolver;
import org.apache.camel.dsl.yaml.deserializers.ModelDeserializers;
import org.snakeyaml.engine.v2.api.ConstructNode;
import org.snakeyaml.engine.v2.nodes.Node;

// TODO: remove it when https://issues.apache.org/jira/browse/CAMEL-16424 get fixed
public class YamlSourceLoaderDeserializerResolver implements YamlDeserializerResolver {
    @Override
    public int getOrder() {
        return YamlDeserializerResolver.ORDER_HIGHEST;
    }

    @Override
    public ConstructNode resolve(String id) {
        switch (id) {
            case "from":
                return new RouteFromDeserializer();
            case "org.apache.camel.model.FromDefinition":
                return new FromDeserializer();
            case "to":
            case "org.apache.camel.model.ToDefinition":
                return new ToDeserializer();
            default:
                return null;
        }
    }

    public static class FromDeserializer extends org.apache.camel.dsl.yaml.deserializers.FromDefinitionDeserializer {
        @Override
        public Object construct(Node node) {
            return super.construct(
                YamlSourceLoaderSupport.properties2parameters(node)
            );
        }
    }
    public static class RouteFromDeserializer extends org.apache.camel.dsl.yaml.deserializers.RouteFromDefinitionDeserializer {
        @Override
        public Object construct(Node node) {
            return super.construct(
                YamlSourceLoaderSupport.properties2parameters(node)
            );
        }
    }
    public static class ToDeserializer extends ModelDeserializers.ToDefinitionDeserializer {
        @Override
        public Object construct(Node node) {
            return super.construct(
                YamlSourceLoaderSupport.properties2parameters(node)
            );
        }
    }
}

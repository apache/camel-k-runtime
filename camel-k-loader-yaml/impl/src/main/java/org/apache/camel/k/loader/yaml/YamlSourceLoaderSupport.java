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

import org.snakeyaml.engine.v2.nodes.MappingNode;
import org.snakeyaml.engine.v2.nodes.Node;
import org.snakeyaml.engine.v2.nodes.NodeTuple;
import org.snakeyaml.engine.v2.nodes.NodeType;
import org.snakeyaml.engine.v2.nodes.ScalarNode;

import static org.apache.camel.dsl.yaml.common.YamlDeserializerSupport.asText;

public final class YamlSourceLoaderSupport {
    private YamlSourceLoaderSupport() {
    }

    /**
     * Workaround for https://issues.apache.org/jira/browse/CAMEL-16424
     *
     * TODO: remove once issue is fixed
     *
     */
    public static Node properties2parameters(Node node) {
        if (node.getNodeType() == NodeType.MAPPING) {
            final MappingNode mn = (MappingNode) node;

            for (int i = 0; i < mn.getValue().size(); i++) {
                final NodeTuple tuple = mn.getValue().get(i);
                final String key = asText(tuple.getKeyNode());

                if ("parameters".equals(key)) {
                    NodeTuple newNode = new NodeTuple(
                        new ScalarNode(
                            tuple.getKeyNode().getTag(),
                            "properties",
                            ((ScalarNode)tuple.getKeyNode()).getScalarStyle()),
                        tuple.getValueNode());

                    mn.getValue().set(i, newNode);

                    break;
                }
            }
        }

        return node;
    }
}

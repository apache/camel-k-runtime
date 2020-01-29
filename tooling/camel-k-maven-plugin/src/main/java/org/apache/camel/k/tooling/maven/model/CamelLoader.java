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
package org.apache.camel.k.tooling.maven.model;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(depluralize = true)
@JsonDeserialize(builder = CamelLoader.Builder.class)
@JsonPropertyOrder({"groupId", "artifactId", "version"})
public interface CamelLoader extends Artifact {
    @Value.Auxiliary
    @Value.Default
    default Set<String> getLanguages() {
        return Collections.emptySet();
    }

    @Value.Auxiliary
    @Value.Default
    default Set<Artifact> getDependencies() {
        return Collections.emptySet();
    }

    @Value.Auxiliary
    @Value.Default
    default Map<String, String> getMetadata() {
        return Collections.emptyMap();
    }

    static Builder fromArtifact(String groupId, String artifactId) {
        return new Builder().groupId(groupId).artifactId(artifactId);
    }

    class Builder extends ImmutableCamelLoader.Builder {
        public Builder addDependency(String groupId, String artifactId) {
            addDependencies(MavenArtifact.from(groupId, artifactId));
            return this;
        }
    }
}

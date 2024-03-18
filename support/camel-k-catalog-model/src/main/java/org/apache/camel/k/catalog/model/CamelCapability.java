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
package org.apache.camel.k.catalog.model;

import java.util.Collections;
import java.util.Optional;
import java.util.SortedSet;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(depluralize = true)
@JsonDeserialize(builder = CamelCapability.Builder.class)
@JsonPropertyOrder({"groupId", "artifactId", "classifier","version"})
public interface CamelCapability {
    @Value.Auxiliary
    @Value.Default
    @Value.NaturalOrder
    default SortedSet<Artifact> getDependencies() {
        return Collections.emptySortedSet();
    }

    @Value.Auxiliary
    @Value.Default
    @Value.NaturalOrder
    default SortedSet<Property> getRuntimeProperties() {
        return Collections.emptySortedSet();
    }

    @Value.Auxiliary
    @Value.Default
    @Value.NaturalOrder
    default SortedSet<Property> getBuildTimeProperties() {
        return Collections.emptySortedSet();
    }

    @Value.Auxiliary
    @Value.Default
    @Value.NaturalOrder
    default SortedSet<Property> getMetadata() {
        return Collections.emptySortedSet();
    }

    static CamelCapability forArtifact(String groupId, String artifactId) {
        return new Builder().addDependency(groupId, artifactId).build();
    }

    class Builder extends ImmutableCamelCapability.Builder {
        public Builder addDependency(String groupId, String artifactId) {
            return super.addDependencies(Artifact.from(groupId, artifactId));
        }

        public Builder addDependency(String groupId, String artifactId, Optional<String> classifier) {
            if (classifier.isEmpty()) {
                return super.addDependencies(Artifact.from(groupId, artifactId));
            } else {
                return super.addDependencies(Artifact.from(groupId, artifactId, classifier.get()));
            }
        }

        public Builder addRuntimeProperty(String key, String value) {
            return super.addRuntimeProperty(Property.from(key, value));
        }

        public Builder addBuildTimeProperty(String key, String value) {
            return super.addBuildTimeProperty(Property.from(key, value));
        }

        public Builder addMetadata(String key, String value) {
            return super.addMetadata(Property.from(key, value));
        }
    }
}

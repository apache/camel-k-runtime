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
package org.apache.camel.k.tooling.maven.processors;

import java.util.Map;

import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.k.tooling.maven.model.Artifact;
import org.apache.camel.k.tooling.maven.model.CamelArtifact;
import org.apache.camel.k.tooling.maven.model.CatalogProcessor;
import org.apache.camel.k.tooling.maven.model.crd.CamelCatalogSpec;
import org.apache.camel.k.tooling.maven.model.crd.RuntimeSpec;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CatalogProcessor3Test extends AbstractCatalogProcessorTest {
    @Test
    public void testAcceptHyphen() {
        CamelCatalog catalog = versionCamelCatalog("3.0.0.acme-123456");

        assertThat(new CatalogProcessor3x().accepts(catalog)).isTrue();
    }

    @Test
    public void testAcceptEqualToLower() {
        CamelCatalog catalog = versionCamelCatalog("3.0.0");

        assertThat(new CatalogProcessor3x().accepts(catalog)).isTrue();
    }

    @Test
    public void testAcceptLessThanLower() {
        CamelCatalog catalog = versionCamelCatalog("2.17.0");

        assertThat(new CatalogProcessor3x().accepts(catalog)).isFalse();
    }

    @Test
    public void testAcceptEqualToHigher() {
        CamelCatalog catalog = versionCamelCatalog("4.0.0");

        assertThat(new CatalogProcessor3x().accepts(catalog)).isFalse();
    }

    @Test
    public void testAcceptMoreThanHigher() {
        CamelCatalog catalog = versionCamelCatalog("5.0.0");

        assertThat(new CatalogProcessor3x().accepts(catalog)).isFalse();
    }

    @Test
    public void testArtifactsEnrichment() {
        CatalogProcessor processor = new CatalogProcessor3x();
        CamelCatalog catalog = versionCamelCatalog("3.0.0");

        RuntimeSpec runtime = new RuntimeSpec.Builder().version("1.0.0").provider("main").applicationClass("unknown").build();
        CamelCatalogSpec.Builder builder = new CamelCatalogSpec.Builder().runtime(runtime);

        assertThat(processor.accepts(catalog)).isTrue();
        processor.process(new MavenProject(), catalog, builder);

        CamelCatalogSpec spec = builder.build();
        Map<String, CamelArtifact> artifactMap = spec.getArtifacts();

        assertThat(artifactMap).containsKeys("camel-k-runtime-health");
        assertThat(artifactMap).containsKeys("camel-k-runtime-http");
        assertThat(artifactMap).containsKeys("camel-k-runtime-webhook");

        assertThat(artifactMap.get("camel-k-runtime-knative")).satisfies(a -> {
            assertThat(a.getDependencies()).anyMatch(
                d -> d.getGroupId().equals("org.apache.camel.k") && d.getArtifactId().equals("camel-knative-api")
            );
            assertThat(a.getDependencies()).anyMatch(
                d -> d.getGroupId().equals("org.apache.camel.k") && d.getArtifactId().equals("camel-knative")
            );
            assertThat(a.getDependencies()).anyMatch(
                d -> d.getGroupId().equals("org.apache.camel.k") && d.getArtifactId().equals("camel-knative-http")
            );
            assertThat(a.getDependencies()).anyMatch(
                d -> d.getGroupId().equals("org.apache.camel.k") && d.getArtifactId().equals("camel-k-loader-yaml")
            );
        });

        assertThat(artifactMap.get("camel-http")).satisfies(a -> {
            assertThat(a.getDependencies()).anyMatch(
                d -> d.getGroupId().equals("org.apache.camel") && d.getArtifactId().equals("camel-file")
            );
        });
    }

    @Test
    public void testArtifactsDoNotContainVersion() {
        CatalogProcessor processor = new CatalogProcessor3x();
        CamelCatalog catalog = versionCamelCatalog("3.0.0");

        RuntimeSpec runtime = new RuntimeSpec.Builder().version("1.0.0").provider("main").applicationClass("unknown").build();
        CamelCatalogSpec.Builder builder = new CamelCatalogSpec.Builder().runtime(runtime);

        assertThat(processor.accepts(catalog)).isTrue();
        processor.process(new MavenProject(), catalog, builder);

        CamelCatalogSpec spec = builder.build();
        Map<String, CamelArtifact> artifactMap = spec.getArtifacts();

        for (Map.Entry<String, CamelArtifact> artifact: artifactMap.entrySet()) {
            assertThat(artifact.getValue().getVersion()).isNotPresent();

            for (Artifact dependency: artifact.getValue().getDependencies()) {
                assertThat(dependency.getVersion()).isNotPresent();
            }
        }
    }
}

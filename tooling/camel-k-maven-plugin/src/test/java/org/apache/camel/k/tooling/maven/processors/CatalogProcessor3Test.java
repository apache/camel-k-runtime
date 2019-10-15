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

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.k.tooling.maven.model.Artifact;
import org.apache.camel.k.tooling.maven.model.CamelArtifact;
import org.apache.camel.k.tooling.maven.model.CatalogProcessor;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CatalogProcessor3Test extends AbstractCataloProcessorTest {
    @Test
    public void testAcceptHyphen() {
        CatalogProcessor3x cp3 = new CatalogProcessor3x();

        CamelCatalog catalog = versionCamelCatalog("3.0.0.acme-123456");

        assertThat(cp3.accepts(catalog)).isTrue();
    }

    @Test
    public void testAcceptEqualToLower() {
        CatalogProcessor3x cp3 = new CatalogProcessor3x();

        CamelCatalog catalog = versionCamelCatalog("3.0.0");

        assertThat(cp3.accepts(catalog)).isTrue();
    }

    @Test
    public void testAcceptLessThanLower() {
        CatalogProcessor3x cp3 = new CatalogProcessor3x();

        CamelCatalog catalog = versionCamelCatalog("2.17.0");

        assertThat(cp3.accepts(catalog)).isFalse();
    }

    @Test
    public void testAcceptEqualToHigher() {
        CatalogProcessor3x cp3 = new CatalogProcessor3x();

        CamelCatalog catalog = versionCamelCatalog("4.0.0");

        assertThat(cp3.accepts(catalog)).isFalse();
    }

    @Test
    public void testAcceptMoreThanHigher() {
        CatalogProcessor3x cp3 = new CatalogProcessor3x();

        CamelCatalog catalog = versionCamelCatalog("5.0.0");

        assertThat(cp3.accepts(catalog)).isFalse();
    }

    @Test
    public void testArtifactsEnrichment() {
        CatalogProcessor processor = new CatalogProcessor3x();
        CamelCatalog catalog = versionCamelCatalog("3.0.0");
        Map<String, CamelArtifact> artifactMap = new HashMap<>();
        artifactMap.put("camel-http", new CamelArtifact());

        assertThat(processor.accepts(catalog)).isTrue();
        processor.process(new MavenProject(), catalog, artifactMap);

        assertThat(artifactMap.get("camel-k-runtime-health")).satisfies(a -> {
            assertThat(a.getDependencies()).anyMatch(
                d -> d.getGroupId().equals("org.apache.camel") && d.getArtifactId().equals("camel-servlet")
            );
            assertThat(a.getDependencies()).anyMatch(
                d -> d.getGroupId().equals("org.apache.camel.k") && d.getArtifactId().equals("camel-k-runtime-servlet")
            );
        });
        assertThat(artifactMap.get("camel-k-runtime-servlet")).satisfies(a -> {
            assertThat(a.getDependencies()).anyMatch(
                d -> d.getGroupId().equals("org.apache.camel") && d.getArtifactId().equals("camel-servlet")
            );
        });

        assertThat(artifactMap.get("camel-k-runtime-knative")).satisfies(a -> {
            assertThat(a.getDependencies()).anyMatch(
                d -> d.getGroupId().equals("org.apache.camel.k") && d.getArtifactId().equals("camel-knative")
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
            assertThat(a.getDependencies()).anyMatch(
                d -> d.getGroupId().equals("org.apache.camel") && d.getArtifactId().equals("camel-cloud")
            );
        });

        assertThat(artifactMap.get("camel-knative")).satisfies(a -> {
            assertThat(a.getDependencies()).anyMatch(
                d -> d.getGroupId().equals("org.apache.camel") && d.getArtifactId().equals("camel-cloud")
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
        Map<String, CamelArtifact> artifactMap = new HashMap<>();
        artifactMap.put("camel-http", new CamelArtifact());

        assertThat(processor.accepts(catalog)).isTrue();
        processor.process(new MavenProject(), catalog, artifactMap);

        for (Map.Entry<String, CamelArtifact> artifact: artifactMap.entrySet()) {
            assertThat(artifact.getValue().getVersion()).isNull();

            for (Artifact dependency: artifact.getValue().getDependencies()) {
                assertThat(dependency.getVersion()).isNull();
            }
        }
    }
}

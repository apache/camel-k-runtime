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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.vdurmont.semver4j.Semver;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.k.catalog.model.CamelArtifact;
import org.apache.camel.k.catalog.model.CamelLoader;
import org.apache.camel.k.catalog.model.CamelScheme;
import org.apache.camel.k.catalog.model.CamelScopedArtifact;
import org.apache.camel.k.catalog.model.CatalogComponentDefinition;
import org.apache.camel.k.catalog.model.CatalogDataFormatDefinition;
import org.apache.camel.k.catalog.model.CatalogDefinition;
import org.apache.camel.k.catalog.model.CatalogLanguageDefinition;
import org.apache.camel.k.catalog.model.CatalogSupport;
import org.apache.camel.k.catalog.model.k8s.crd.CamelCatalogSpec;
import org.apache.camel.k.tooling.maven.support.CatalogProcessor;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.project.MavenProject;

public class CatalogProcessor3x implements CatalogProcessor {
    private static final List<String> KNOWN_HTTP_URIS = Arrays.asList(
        "ahc",
        "ahc-ws",
        "atmosphere-websocket",
        "cxf",
        "cxfrs",
        "grpc",
        "jetty",
        "netty-http",
        "platform-http",
        "rest",
        "restlet",
        "servlet",
        "spark-rest",
        "spring-ws",
        "undertow",
        "webhook",
        "websocket"
    );

    private static final List<String> KNOWN_PASSIVE_URIS = Arrays.asList(
        "bean",
        "binding",
        "browse",
        "class",
        "controlbus",
        "dataformat",
        "dataset",
        "direct",
        "direct-vm",
        "language",
        "log",
        "mock",
        "ref",
        "seda",
        "stub",
        "test",
        "validator",
        "vm"
    );

    @Override
    public int getOrder() {
        return HIGHEST;
    }

    @Override
    public boolean accepts(CamelCatalog catalog) {
        Semver semver = new Semver(catalog.getCatalogVersion(), Semver.SemverType.IVY);
        return semver.isGreaterThan("2.999.999") && semver.isLowerThan("4.0.0");
    }

    @Override
    public void process(MavenProject project, CamelCatalog catalog, CamelCatalogSpec.Builder specBuilder,
                        List<String> exclusions) {
        Map<String, CamelArtifact> artifacts = new TreeMap<>();

        processComponents(catalog, artifacts, exclusions);
        processLanguages(catalog, artifacts);
        processDataFormats(catalog, artifacts);
        processLoaders(specBuilder);

        artifacts.computeIfPresent("camel-http",
            (key, artifact) -> new CamelArtifact.Builder()
                .from(artifact)
                .addDependency("org.apache.camel", "camel-file")
                .build()
        );

        specBuilder.putAllArtifacts(artifacts);

        specBuilder.putArtifact(
            new CamelArtifact.Builder()
                .groupId("org.apache.camel.k")
                .artifactId("camel-k-cron")
                .build()
        );
        specBuilder.putArtifact(
            new CamelArtifact.Builder()
                .groupId("org.apache.camel.k")
                .artifactId("camel-k-webhook")
                .build()
        );
        specBuilder.putArtifact(
            new CamelArtifact.Builder()
                .groupId("org.apache.camel.k")
                .artifactId("camel-k-master")
                .build()
        );

        specBuilder.putArtifact(
            new CamelArtifact.Builder()
                .groupId("org.apache.camel.k")
                .artifactId("camel-k-knative")
                .addScheme(new CamelScheme.Builder()
                    .id("knative")
                    .http(true)
                    .consumer(new CamelScopedArtifact.Builder()
                        .addDependency("org.apache.camel.k", "camel-k-knative-consumer")
                        .build())
                    .producer(new CamelScopedArtifact.Builder()
                        .addDependency("org.apache.camel.k", "camel-k-knative-producer")
                        .build())
                    .build())
                .build()
        );
    }

    private static void processLoaders(CamelCatalogSpec.Builder specBuilder) {
        specBuilder.putLoader(
            "yaml",
            CamelLoader.fromArtifact("org.apache.camel.k", "camel-k-loader-yaml")
                .addLanguage("yaml")
                .putMetadata("native", "true")
                .build()
        );
        specBuilder.putLoader(
            "groovy",
            CamelLoader.fromArtifact("org.apache.camel.quarkus", "camel-quarkus-groovy-dsl")
                .addLanguage("groovy")
                .putMetadata("native", "false")
                .build()
        );
        specBuilder.putLoader(
            "kts",
            CamelLoader.fromArtifact("org.apache.camel.quarkus", "camel-quarkus-kotlin-dsl")
                .addLanguage("kts")
                .putMetadata("native", "false")
                .build()
        );
        specBuilder.putLoader(
            "js",
            CamelLoader.fromArtifact("org.apache.camel.quarkus", "camel-quarkus-js-dsl")
                .addLanguage("js")
                .putMetadata("native", "true")
                .build()
        );
        specBuilder.putLoader(
            "xml",
            CamelLoader.fromArtifact("org.apache.camel.quarkus", "camel-quarkus-xml-io-dsl")
                .addLanguage("xml")
                .putMetadata("native", "true")
                .build()
        );
        specBuilder.putLoader(
            "java",
            CamelLoader.fromArtifact("org.apache.camel.quarkus", "camel-quarkus-java-joor-dsl")
                .addLanguages("java")
                .putMetadata("native", "false")
                .build()
        );
        specBuilder.putLoader(
            "jsh",
            CamelLoader.fromArtifact("org.apache.camel.k", "camel-k-loader-jsh")
                .addLanguages("jsh")
                .putMetadata("native", "false")
                .build()
        );
    }

    private static void processComponents(CamelCatalog catalog, Map<String, CamelArtifact> artifacts, List<String> exclusions) {
        final Set<String> elements = new TreeSet<>(catalog.findComponentNames());

        elements.removeAll(exclusions);

        for (String name : elements) {
            String json = catalog.componentJSonSchema(name);
            CatalogComponentDefinition definition = CatalogSupport.unmarshallComponent(json);

            artifacts.compute(definition.getArtifactId(), (key, artifact) -> {
                CamelArtifact.Builder builder = artifactBuilder(artifact, definition);
                builder.addJavaType(definition.getJavaType());

                definition.getSchemes().map(StringUtils::trimToNull).filter(Objects::nonNull).forEach(scheme -> {
                    builder.addScheme(
                        new CamelScheme.Builder()
                            .id(scheme)
                            .http(KNOWN_HTTP_URIS.contains(scheme))
                            .passive(KNOWN_PASSIVE_URIS.contains(scheme))
                            .build());
                });

                return builder.build();
            });
        }
    }

    private static void processLanguages(CamelCatalog catalog, Map<String, CamelArtifact> artifacts) {
        final Set<String> elements = new TreeSet<>(catalog.findLanguageNames());

        for (String name : elements) {
            String json = catalog.languageJSonSchema(name);
            CatalogLanguageDefinition definition = CatalogSupport.unmarshallLanguage(json);

            artifacts.compute(definition.getArtifactId(), (key, artifact) -> {
                CamelArtifact.Builder builder = artifactBuilder(artifact, definition);
                builder.addLanguage(definition.getName());
                builder.addJavaType(definition.getJavaType());

                return builder.build();
            });
        }
    }

    private static void processDataFormats(CamelCatalog catalog, Map<String, CamelArtifact> artifacts) {
        final Set<String> elements = new TreeSet<>(catalog.findDataFormatNames());

        for (String name : elements) {
            String json = catalog.dataFormatJSonSchema(name);
            CatalogDataFormatDefinition definition = CatalogSupport.unmarshallDataFormat(json);

            artifacts.compute(definition.getArtifactId(), (key, artifact) -> {
                CamelArtifact.Builder builder = artifactBuilder(artifact, definition);
                builder.addDataformat(definition.getName());
                builder.addJavaType(definition.getJavaType());

                return builder.build();
            });
        }
    }

    private static CamelArtifact.Builder artifactBuilder(CamelArtifact artifact, CatalogDefinition definition) {
        CamelArtifact.Builder builder = new  CamelArtifact.Builder();

        if (artifact != null) {
            builder.from(artifact);
        } else {
            Objects.requireNonNull(definition.getGroupId());
            Objects.requireNonNull(definition.getArtifactId());

            builder.groupId(definition.getGroupId());
            builder.artifactId(definition.getArtifactId());
        }

        return builder;
    }
}

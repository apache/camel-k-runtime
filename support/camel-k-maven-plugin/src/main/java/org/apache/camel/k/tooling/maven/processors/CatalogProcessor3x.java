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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.vdurmont.semver4j.Semver;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.k.tooling.maven.model.CamelArtifact;
import org.apache.camel.k.tooling.maven.model.CamelLoader;
import org.apache.camel.k.tooling.maven.model.CamelScheme;
import org.apache.camel.k.tooling.maven.model.CatalogComponentDefinition;
import org.apache.camel.k.tooling.maven.model.CatalogDataFormatDefinition;
import org.apache.camel.k.tooling.maven.model.CatalogDefinition;
import org.apache.camel.k.tooling.maven.model.CatalogLanguageDefinition;
import org.apache.camel.k.tooling.maven.model.CatalogProcessor;
import org.apache.camel.k.tooling.maven.model.CatalogSupport;
import org.apache.camel.k.tooling.maven.model.MavenArtifact;
import org.apache.camel.k.tooling.maven.model.crd.CamelCatalogSpec;
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
    public void process(MavenProject project, CamelCatalog catalog, CamelCatalogSpec.Builder specBuilder) {
        Map<String, CamelArtifact> artifacts = new HashMap<>();

        processComponents(catalog, artifacts);
        processLanguages(catalog, artifacts);
        processDataFormats(catalog, artifacts);
        processLoaders(specBuilder);

        artifacts.computeIfPresent("camel-http",
            (key, artifact) -> new CamelArtifact.Builder()
                .from(artifact)
                .addDependencies(MavenArtifact.from("org.apache.camel", "camel-file"))
                .build()
        );

        specBuilder.putAllArtifacts(artifacts);

        specBuilder.putArtifact(
            new CamelArtifact.Builder()
                .groupId("org.apache.camel.k")
                .artifactId("camel-k-cron")
                .addScheme(new CamelScheme.Builder()
                    .id("cron")
                    .http(true)
                    .build())
                .build()
        );
        specBuilder.putArtifact(
            new CamelArtifact.Builder()
                .groupId("org.apache.camel.k")
                .artifactId("camel-k-kamelet")
                .addScheme(new CamelScheme.Builder()
                    .id("kamelet")
                    .http(false)
                    .passive(true)
                    .build())
                .build()
        );
        specBuilder.putArtifact(
            new CamelArtifact.Builder()
                .groupId("org.apache.camel.k")
                .artifactId("camel-k-knative")
                .addScheme(new CamelScheme.Builder()
                    .id("knative")
                    .http(true)
                    .build())
                .build()
        );
        specBuilder.putArtifact(
            new CamelArtifact.Builder()
                .groupId("org.apache.camel.k")
                .artifactId("camel-k-webhook")
                .addScheme(new CamelScheme.Builder()
                    .id("wrap")
                    .build())
                .build()
        );
        specBuilder.putArtifact(
            new CamelArtifact.Builder()
                .groupId("org.apache.camel.k")
                .artifactId("camel-k-master")
                .addScheme(new CamelScheme.Builder()
                    .id("master")
                    .build())
                .build()
        );
        specBuilder.putArtifact(
            new CamelArtifact.Builder()
                .groupId("org.apache.camel.k")
                .artifactId("camel-k-webhook")
                .addScheme(new CamelScheme.Builder()
                    .id("webhook")
                    .http(true)
                    .passive(true)
                    .build())
                .build()
        );
        specBuilder.putArtifact(
            new CamelArtifact.Builder()
                .groupId("org.apache.camel.k")
                .artifactId("camel-k-wrap")
                .addScheme(new CamelScheme.Builder()
                    .id("wrap")
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
            CamelLoader.fromArtifact("org.apache.camel.k", "camel-k-loader-groovy")
                .addLanguage("groovy")
                .putMetadata("native", "false")
                .build()
        );
        specBuilder.putLoader(
            "kts",
            CamelLoader.fromArtifact("org.apache.camel.k", "camel-k-loader-kotlin")
                .addLanguage("kts")
                .putMetadata("native", "false")
                .build()
        );
        specBuilder.putLoader(
            "js",
            CamelLoader.fromArtifact("org.apache.camel.k", "camel-k-loader-js")
                .addLanguage("js")
                .putMetadata("native", "true")
                .build()
        );
        specBuilder.putLoader(
            "xml",
            CamelLoader.fromArtifact("org.apache.camel.k", "camel-k-loader-xml")
                .addLanguage("xml")
                .putMetadata("native", "true")
                .build()
        );
        specBuilder.putLoader(
            "java",
            CamelLoader.fromArtifact("org.apache.camel.k", "camel-k-loader-java")
                .addLanguage("java")
                .putMetadata("native", "false")
                .build()
        );
    }

    private static void processComponents(CamelCatalog catalog, Map<String, CamelArtifact> artifacts) {
        for (String name : catalog.findComponentNames()) {
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
        for (String name : catalog.findLanguageNames()) {
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
        for (String name : catalog.findDataFormatNames()) {
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

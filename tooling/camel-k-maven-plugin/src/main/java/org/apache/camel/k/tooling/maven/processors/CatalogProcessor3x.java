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

import com.vdurmont.semver4j.Semver;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.k.tooling.maven.model.CamelArtifact;
import org.apache.camel.k.tooling.maven.model.CatalogProcessor;
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
        "properties",
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
    public void process(MavenProject project, CamelCatalog catalog, Map<String, CamelArtifact> artifacts) {

        // ************************
        //
        // camel-k-runtime-main
        //
        // ************************

        {
            CamelArtifact artifact = new CamelArtifact();
            artifact.setGroupId("org.apache.camel.k");
            artifact.setArtifactId("camel-k-runtime-main");
            artifact.setVersion(project.getVersion());
            artifact.addDependency("org.apache.camel", "camel-core-engine");
            artifact.addDependency("org.apache.camel", "camel-main");
            artifact.addDependency("org.apache.camel", "camel-properties");

            artifacts.put(artifact.getArtifactId(), artifact);
        }

        // ************************
        //
        // camel-k-loader-groovy
        //
        // ************************

        {
            CamelArtifact artifact = new CamelArtifact();
            artifact.setGroupId("org.apache.camel.k");
            artifact.setArtifactId("camel-k-loader-groovy");
            artifact.setVersion(project.getVersion());
            artifact.addDependency("org.apache.camel", "camel-endpointdsl");
            artifact.addDependency("org.apache.camel", "camel-groovy");

            artifacts.put(artifact.getArtifactId(), artifact);
        }

        // ************************
        //
        // camel-k-loader-kotlin
        //
        // ************************

        {
            CamelArtifact artifact = new CamelArtifact();
            artifact.setGroupId("org.apache.camel.k");
            artifact.setArtifactId("camel-k-loader-kotlin");
            artifact.setVersion(project.getVersion());
            artifact.addDependency("org.apache.camel", "camel-endpointdsl");

            artifacts.put(artifact.getArtifactId(), artifact);
        }

        // ************************
        //
        // camel-k-loader-js
        //
        // ************************

        {
            CamelArtifact artifact = new CamelArtifact();
            artifact.setGroupId("org.apache.camel.k");
            artifact.setArtifactId("camel-k-loader-js");
            artifact.setVersion(project.getVersion());
            artifact.addDependency("org.apache.camel", "camel-endpointdsl");

            artifacts.put(artifact.getArtifactId(), artifact);
        }

        // ************************
        //
        // camel-k-loader-xml
        //
        // ************************

        {
            CamelArtifact artifact = new CamelArtifact();
            artifact.setGroupId("org.apache.camel.k");
            artifact.setArtifactId("camel-k-loader-xml");
            artifact.setVersion(project.getVersion());

            artifacts.put(artifact.getArtifactId(), artifact);
        }

        // ************************
        //
        // camel-k-loader-java
        //
        // ************************

        {
            CamelArtifact artifact = new CamelArtifact();
            artifact.setGroupId("org.apache.camel.k");
            artifact.setArtifactId("camel-k-loader-java");
            artifact.setVersion(project.getVersion());
            artifact.addDependency("org.apache.camel", "camel-endpointdsl");

            artifacts.put(artifact.getArtifactId(), artifact);
        }

        // ************************
        //
        // camel-k-loader-knative
        //
        // ************************

        {
            CamelArtifact artifact = new CamelArtifact();
            artifact.setGroupId("org.apache.camel.k");
            artifact.setArtifactId("camel-k-loader-knative");
            artifact.setVersion(project.getVersion());

            artifacts.put(artifact.getArtifactId(), artifact);
        }

        // ************************
        //
        // camel-knative
        //
        // ************************

        {
            CamelArtifact artifact = new CamelArtifact();
            artifact.setGroupId("org.apache.camel.k");
            artifact.setArtifactId("camel-knative");
            artifact.setVersion(project.getVersion());
            artifact.createScheme("knative").setHttp(true);
            artifact.addDependency("org.apache.camel", "camel-cloud");
            artifact.addDependency("org.apache.camel.k", "camel-knative-api");
            artifact.addDependency("org.apache.camel.k", "camel-knative-http");

            artifacts.put(artifact.getArtifactId(), artifact);
        }

        // ************************
        //
        // camel-k-runtime-health
        //
        // ************************

        {
            CamelArtifact artifact = new CamelArtifact();
            artifact.setGroupId("org.apache.camel.k");
            artifact.setArtifactId("camel-k-runtime-health");
            artifact.addDependency("org.apache.camel", "camel-servlet");
            artifact.addDependency("org.apache.camel.k", "camel-k-runtime-servlet");


            artifacts.put(artifact.getArtifactId(), artifact);
        }

        // ************************
        //
        // camel-k-runtime-servlet
        //
        // ************************

        {
            CamelArtifact artifact = new CamelArtifact();
            artifact.setGroupId("org.apache.camel.k");
            artifact.setArtifactId("camel-k-runtime-servlet");
            artifact.addDependency("org.apache.camel", "camel-servlet");

            artifacts.put(artifact.getArtifactId(), artifact);
        }

        // ************************
        //
        // camel-k-runtime-knative
        //
        // ************************

        {
            CamelArtifact artifact = new CamelArtifact();
            artifact.setGroupId("org.apache.camel.k");
            artifact.setArtifactId("camel-k-runtime-knative");
            artifact.addDependency("org.apache.camel", "camel-cloud");
            artifact.addDependency("org.apache.camel.k", "camel-k-loader-yaml");
            artifact.addDependency("org.apache.camel.k", "camel-k-loader-knative");
            artifact.addDependency("org.apache.camel.k", "camel-knative-api");
            artifact.addDependency("org.apache.camel.k", "camel-knative");
            artifact.addDependency("org.apache.camel.k", "camel-knative-http");

            artifacts.put(artifact.getArtifactId(), artifact);
        }

        // ************************
        //
        // legacy
        //
        // ************************

        {
            CamelArtifact artifact = new CamelArtifact();
            artifact.setGroupId("org.apache.camel.k");
            artifact.setArtifactId("camel-k-runtime-jvm");
            artifact.setVersion(project.getVersion());
            artifact.addDependency("org.apache.camel.k", "camel-k-runtime-main");
            artifact.addDependency("org.apache.camel.k", "camel-k-loader-js");
            artifact.addDependency("org.apache.camel.k", "camel-k-loader-xml");
            artifact.addDependency("org.apache.camel.k", "camel-k-loader-java");
            artifact.addDependency("org.apache.camel", "camel-core-engine");
            artifact.addDependency("org.apache.camel", "camel-main");
            artifact.addDependency("org.apache.camel", "camel-properties");

            artifacts.put(artifact.getArtifactId(), artifact);
        }

        {
            CamelArtifact artifact = new CamelArtifact();
            artifact.setGroupId("org.apache.camel.k");
            artifact.setArtifactId("camel-k-runtime-groovy");
            artifact.setVersion(project.getVersion());
            artifact.addDependency("org.apache.camel.k", "camel-k-loader-groovy");
            artifact.addDependency("org.apache.camel", "camel-groovy");

            artifacts.put(artifact.getArtifactId(), artifact);
        }

        {
            CamelArtifact artifact = new CamelArtifact();
            artifact.setGroupId("org.apache.camel.k");
            artifact.setArtifactId("camel-k-runtime-kotlin");
            artifact.setVersion(project.getVersion());
            artifact.addDependency("org.apache.camel.k", "camel-k-loader-kotlin");

            artifacts.put(artifact.getArtifactId(), artifact);
        }

        {
            CamelArtifact artifact = new CamelArtifact();
            artifact.setGroupId("org.apache.camel.k");
            artifact.setArtifactId("camel-k-runtime-yaml");
            artifact.setVersion(project.getVersion());
            artifact.addDependency("org.apache.camel.k", "camel-k-loader-yaml");

            artifacts.put(artifact.getArtifactId(), artifact);
        }

        // ************************
        //
        //
        //
        // ************************

        for (String scheme: KNOWN_HTTP_URIS) {
            artifacts.values().forEach(artifact -> artifact.getScheme(scheme).ifPresent(s -> s.setHttp(true)));
        }
        for (String scheme: KNOWN_PASSIVE_URIS) {
            artifacts.values().forEach(artifact -> artifact.getScheme(scheme).ifPresent(s -> s.setPassive(true)));
        }

        // ************************
        //
        //
        //
        // ************************

        artifacts.computeIfPresent("camel-http", (key, artifact) -> {
            artifact.addDependency("org.apache.camel", "camel-file");
            return artifact;
        });
    }
}

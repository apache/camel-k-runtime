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
package org.apache.camel.k.tooling.maven;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.catalog.quarkus.QuarkusRuntimeProvider;
import org.apache.camel.impl.engine.AbstractCamelContext;
import org.apache.camel.k.catalog.model.Artifact;
import org.apache.camel.k.catalog.model.CamelArtifact;
import org.apache.camel.k.catalog.model.CamelCapability;
import org.apache.camel.k.catalog.model.CamelLoader;
import org.apache.camel.k.catalog.model.CamelScheme;
import org.apache.camel.k.catalog.model.CatalogComponentDefinition;
import org.apache.camel.k.catalog.model.CatalogDataFormatDefinition;
import org.apache.camel.k.catalog.model.CatalogDefinition;
import org.apache.camel.k.catalog.model.CatalogLanguageDefinition;
import org.apache.camel.k.catalog.model.CatalogOtherDefinition;
import org.apache.camel.k.catalog.model.CatalogSupport;
import org.apache.camel.k.catalog.model.Property;
import org.apache.camel.k.catalog.model.k8s.ObjectMeta;
import org.apache.camel.k.catalog.model.k8s.crd.CamelCatalog;
import org.apache.camel.k.catalog.model.k8s.crd.CamelCatalogSpec;
import org.apache.camel.k.catalog.model.k8s.crd.RuntimeSpec;
import org.apache.camel.k.tooling.maven.support.MavenSupport;
import org.apache.camel.quarkus.core.FastCamelContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(
    name = "generate-catalog",
    defaultPhase = LifecyclePhase.GENERATE_RESOURCES,
    threadSafe = true,
    requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
    requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class GenerateCatalogMojo extends AbstractMojo {
    private static final List<String> KNOWN_HTTP_URIS = Arrays.asList(
        "ahc",
        "ahc-ws",
        "atmosphere-websocket",
        "cxf",
        "cxfrs",
        "grpc",
        "jetty",
        "knative",
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

    @Parameter(property = "catalog.path", defaultValue = "${project.build.directory}")
    private String outputPath;

    @Parameter(property = "catalog.file", defaultValue = "camel-catalog-${runtime.version}.yaml")
    private String outputFile;

    @Parameter(property = "components.exclusion.list")
    private Set<String> componentsExclusionList;

    @Parameter(property = "dataformats.exclusion.list")
    private Set<String> dataformatsExclusionList;

    @Parameter(property = "languages.exclusion.list")
    private Set<String> languagesExclusionList;

    @Parameter(property = "others.exclusion.list")
    private Set<String> othersExclusionList;

    @Parameter(property = "dsls.exclusion.list")
    private Set<String> dslsExclusionList;

    @Parameter(property = "capabilities.exclusion.list")
    private Set<String> capabilitiesExclusionList;

    // ********************
    //
    // ********************

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final Path output = Paths.get(this.outputPath, this.outputFile);

        try {
            if (Files.notExists(output.getParent())) {
                Files.createDirectories(output.getParent());
            }
            if (Files.exists(output)) {
                Files.delete(output);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Exception while generating camel catalog", e);
        }

        final org.apache.camel.catalog.CamelCatalog catalog = new DefaultCamelCatalog();
        catalog.setRuntimeProvider(new QuarkusRuntimeProvider());

        final String runtimeVersion = MavenSupport.getVersion(getClass(), "/META-INF/maven/org.apache.camel.k/camel-k-maven-plugin/pom.properties");
        final String catalogName = String.format("camel-catalog-%s", runtimeVersion.toLowerCase(Locale.US));

        try {
            CamelCatalogSpec.Builder catalogSpec = new CamelCatalogSpec.Builder();

            RuntimeSpec.Builder runtimeSpec = new RuntimeSpec.Builder()
                .version(runtimeVersion)
                .provider("quarkus");

            MavenSupport.getVersion(
                AbstractCamelContext.class,
                "org.apache.camel", "camel-base",
                version -> runtimeSpec.putMetadata("camel.version", version));
            MavenSupport.getVersion(
                FastCamelContext.class,
                "io.quarkus", "quarkus-core",
                version -> runtimeSpec.putMetadata("quarkus.version", version));
            MavenSupport.getVersion(
                QuarkusRuntimeProvider.class,
                "org.apache.camel.quarkus", "camel-quarkus-catalog",
                version -> runtimeSpec.putMetadata("camel-quarkus.version", version));

            runtimeSpec.putMetadata("quarkus.native-builder-image", MavenSupport.getApplicationProperty(getClass(), "quarkus.native-builder-image"));

            runtimeSpec.putMetadata("jib.maven-plugin.version",
                MavenSupport.getApplicationProperty(getClass(), "jib.maven-plugin.version"));
            runtimeSpec.putMetadata("jib.layer-filter-extension-maven.version",
                MavenSupport.getApplicationProperty(getClass(), "jib.layer-filter-extension-maven.version"));

            runtimeSpec.applicationClass("io.quarkus.bootstrap.runner.QuarkusEntryPoint");
            runtimeSpec.addDependency("org.apache.camel.k", "camel-k-runtime");

            addCapabilities(runtimeSpec, catalogSpec);

            catalogSpec.runtime(runtimeSpec.build());

            process(catalog, catalogSpec);

            ObjectMeta.Builder metadata = new ObjectMeta.Builder()
                .name(catalogName)
                .putLabels("app", "camel-k")
                .putLabels("camel.apache.org/catalog.version", catalog.getCatalogVersion())
                .putLabels("camel.apache.org/catalog.loader.version", catalog.getLoadedVersion())
                .putLabels("camel.apache.org/runtime.version", runtimeVersion);

            CamelCatalog cr = new CamelCatalog.Builder()
                .metadata(metadata.build())
                .spec(catalogSpec.build())
                .build();

            //
            // apiVersion: camel.apache.org/v1
            // kind: CamelCatalog
            // metadata:
            //   name: catalog-x.y.z-main
            //   labels:
            //     app: "camel-k"
            //     camel.apache.org/catalog.version: x.y.x
            //     camel.apache.org/catalog.loader.version: x.y.z
            //     camel.apache.org/runtime.version: x.y.x
            //     camel.apache.org/runtime.provider: main
            // spec:
            //   version:
            //   runtimeVersion:
            // status:
            //   artifacts:
            //
            try (Writer writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {

                YAMLFactory factory = new YAMLFactory()
                    .configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, true)
                    .configure(YAMLGenerator.Feature.ALWAYS_QUOTE_NUMBERS_AS_STRINGS, true)
                    .configure(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID, false)
                    .configure(YAMLGenerator.Feature.WRITE_DOC_START_MARKER, false);

                // write license header
                writer.write(
                    GenerateSupport.getResourceAsString("/catalog-license.txt")
                );

                getLog().info("Writing catalog file to: " + output);

                // write catalog data
                ObjectMapper mapper = new ObjectMapper(factory);
                mapper.registerModule(new Jdk8Module());
                mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
                mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
                mapper.writeValue(writer, cr);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Exception while generating catalog", e);
        }
    }

    // ********************
    //
    // ********************

    public void process(
        org.apache.camel.catalog.CamelCatalog catalog,
        CamelCatalogSpec.Builder specBuilder) {

        Map<String, CamelArtifact> artifacts = new TreeMap<>();

        processComponents(catalog, artifacts);
        processLanguages(catalog, artifacts);
        processDataFormats(catalog, artifacts);
        processOthers(catalog, artifacts);
        processLoaders(specBuilder);

        specBuilder.putAllArtifacts(artifacts);
    }

    private void processLoaders(CamelCatalogSpec.Builder specBuilder) {
        if (dslsExclusionList != null) {
            getLog().info("dsls.exclusion.list: " + dslsExclusionList);
        }

        if (dslsExclusionList != null && !dslsExclusionList.contains("yaml")) {
            specBuilder.putLoader(
                "yaml",
                CamelLoader.fromArtifact("org.apache.camel.quarkus", "camel-quarkus-yaml-dsl")
                    .addLanguage("yaml")
                    .putMetadata("native", "true")
                    .build()
            );
        }
        if (dslsExclusionList != null && !dslsExclusionList.contains("groovy")) {
            specBuilder.putLoader(
                "groovy",
                CamelLoader.fromArtifact("org.apache.camel.quarkus", "camel-quarkus-groovy-dsl")
                    .addLanguage("groovy")
                    .putMetadata("native", "true")
                    .putMetadata("sources-required-at-build-time", "true")
                    .build()
            );
        }
        if (dslsExclusionList != null && !dslsExclusionList.contains("kts")) {
            specBuilder.putLoader(
                "kts",
                CamelLoader.fromArtifact("org.apache.camel.quarkus", "camel-quarkus-kotlin-dsl")
                    .addLanguage("kts")
                    .putMetadata("native", "true")
                    .putMetadata("sources-required-at-build-time", "true")
                    .build()
            );
        }
        if (dslsExclusionList != null && !dslsExclusionList.contains("js")) {
            specBuilder.putLoader(
                "js",
                CamelLoader.fromArtifact("org.apache.camel.quarkus", "camel-quarkus-js-dsl")
                    .addLanguage("js")
                    // Guest languages are not yet supported on Mandrel in native mode.
                    .putMetadata("native", "false")
                    .build()
            );
        }
        if (dslsExclusionList != null && !dslsExclusionList.contains("xml")) {
            specBuilder.putLoader(
                "xml",
                CamelLoader.fromArtifact("org.apache.camel.quarkus", "camel-quarkus-xml-io-dsl")
                    .addLanguage("xml")
                    .putMetadata("native", "true")
                    .build()
            );
        }
        if (dslsExclusionList != null && !dslsExclusionList.contains("java")) {
            specBuilder.putLoader(
                "java",
                CamelLoader.fromArtifact("org.apache.camel.quarkus", "camel-quarkus-java-joor-dsl")
                    .addLanguages("java")
                    .putMetadata("native", "true")
                    .putMetadata("sources-required-at-build-time", "true")
                    .build()
            );
        }
        if (dslsExclusionList != null && !dslsExclusionList.contains("jsh")) {
            specBuilder.putLoader(
                "jsh",
                CamelLoader.fromArtifact("org.apache.camel.quarkus", "camel-quarkus-jsh-dsl")
                    .addLanguages("jsh")
                    // Native mode is not yet supported due to https://github.com/apache/camel-quarkus/issues/4458.
                    .putMetadata("native", "false")
                    .putMetadata("sources-required-at-build-time", "true")
                    .build()
            );
        }
    }

    private void processComponents(org.apache.camel.catalog.CamelCatalog catalog, Map<String, CamelArtifact> artifacts) {
        final Set<String> elements = new TreeSet<>(catalog.findComponentNames());

        if (componentsExclusionList != null) {
            getLog().info("components.exclusion.list: " + componentsExclusionList);
            elements.removeAll(componentsExclusionList);
        }

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

    private void processLanguages(org.apache.camel.catalog.CamelCatalog catalog, Map<String, CamelArtifact> artifacts) {
        final Set<String> elements = new TreeSet<>(catalog.findLanguageNames());

        if (languagesExclusionList != null) {
            getLog().info("languages.exclusion.list: " + languagesExclusionList);
            elements.removeAll(languagesExclusionList);
        }

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

    private void processDataFormats(org.apache.camel.catalog.CamelCatalog catalog, Map<String, CamelArtifact> artifacts) {
        final Set<String> elements = new TreeSet<>(catalog.findDataFormatNames());

        if (dataformatsExclusionList != null) {
            getLog().info("dataformats.exclusion.list: " + dataformatsExclusionList);
            elements.removeAll(dataformatsExclusionList);
        }

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

    private void processOthers(org.apache.camel.catalog.CamelCatalog catalog, Map<String, CamelArtifact> artifacts) {
        final Set<String> elements = new TreeSet<>(catalog.findOtherNames());

        if (othersExclusionList != null) {
            getLog().info("others.exclusion.list: " + othersExclusionList);
            elements.removeAll(othersExclusionList);
        }

        for (String name : elements) {
            String json = catalog.otherJSonSchema(name);
            CatalogOtherDefinition definition = CatalogSupport.unmarshallOther(json);

            artifacts.compute(definition.getArtifactId(), (key, artifact) -> artifactBuilder(artifact, definition).build());
        }
    }

    private CamelArtifact.Builder artifactBuilder(CamelArtifact artifact, CatalogDefinition definition) {
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

    private void addCapabilities(RuntimeSpec.Builder runtimeSpec, CamelCatalogSpec.Builder catalogSpec) {
        List<Artifact> artifacts = new ArrayList<>();
        artifacts.add(Artifact.from("org.apache.camel.k", "camel-k-cron"));
        addCapabilityAndDependecies(runtimeSpec, catalogSpec, "cron", artifacts, true);

        artifacts.clear();
        artifacts.add(Artifact.from("org.apache.camel.quarkus", "camel-quarkus-microprofile-health"));
        List<Property> properties = new ArrayList<>();
        properties.add(Property.from("defaultLivenessProbePath", "/q/health/live"));
        properties.add(Property.from("defaultReadinessProbePath", "/q/health/ready"));
        properties.add(Property.from("defaultStartupProbePath", "/q/health/started"));
        addCapability(runtimeSpec, catalogSpec, "health", artifacts, new ArrayList<>(), new ArrayList<>(), properties, false);

        artifacts.clear();
        artifacts.add(Artifact.from("org.apache.camel.quarkus", "camel-quarkus-platform-http"));
        addCapabilityAndDependecies(runtimeSpec, catalogSpec, "platform-http", artifacts, false);

        artifacts.clear();
        artifacts.add(Artifact.from("org.apache.camel.quarkus", "camel-quarkus-rest"));
        artifacts.add(Artifact.from("org.apache.camel.quarkus", "camel-quarkus-platform-http"));
        addCapabilityAndDependecies(runtimeSpec, catalogSpec, "rest", artifacts, false);

        artifacts.clear();
        artifacts.add(Artifact.from("org.apache.camel.quarkus", "camel-quarkus-microprofile-fault-tolerance"));
        addCapabilityAndDependecies(runtimeSpec, catalogSpec, "circuit-breaker", artifacts, false);

        // Telemetry capability
        artifacts.clear();
        artifacts.add(Artifact.from("org.apache.camel.quarkus", "camel-quarkus-opentelemetry"));
        properties.clear();
        properties.add(Property.from("quarkus.opentelemetry.tracer.exporter.otlp.endpoint", "${camel.k.telemetry.endpoint}"));
        properties.add(Property.from("quarkus.opentelemetry.tracer.resource-attributes", "${camel.k.telemetry.serviceName}"));
        properties.add(Property.from("quarkus.opentelemetry.tracer.sampler", "${camel.k.telemetry.sampler}"));
        properties.add(Property.from("quarkus.opentelemetry.tracer.sampler.ratio", "${camel.k.telemetry.samplerRatio}"));
        properties.add(Property.from("quarkus.opentelemetry.tracer.sampler.parent-based", "${camel.k.telemetry.samplerParentBased}"));
        addCapability(runtimeSpec, catalogSpec, "telemetry", artifacts, properties, new ArrayList<>(), new ArrayList<>(), false);

        artifacts.clear();
        artifacts.add(Artifact.from("org.apache.camel.k", "camel-k-resume-kafka"));
        addCapabilityAndDependecies(runtimeSpec, catalogSpec, "resume-kafka", artifacts, true);

        // Master capability
        artifacts.clear();
        artifacts.add(Artifact.from("org.apache.camel.k", "camel-k-master"));
        properties.clear();
        properties.add(Property.from("quarkus.camel.cluster.kubernetes.resource-name", "${camel.k.master.resourceName}"));
        properties.add(Property.from("quarkus.camel.cluster.kubernetes.resource-type", "${camel.k.master.resourceType}"));
        properties.add(Property.from("quarkus.camel.cluster.kubernetes.labels.\"${camel.k.master.labelKey}\"", "${camel.k.master.labelValue}"));
        List<Property> buildTimeProps = new ArrayList<>();
        buildTimeProps.add(Property.from("quarkus.camel.cluster.kubernetes.enabled", "${camel.k.master.enabled}"));
        addCapability(runtimeSpec, catalogSpec, "master", artifacts, properties, buildTimeProps, new ArrayList<>(), true);

        artifacts.clear();
        artifacts.add(Artifact.from("org.apache.camel.quarkus", "camel-quarkus-hashicorp-vault"));
        addCapabilityAndDependecies(runtimeSpec, catalogSpec, "hashicorp-vault", artifacts, false);

        artifacts.clear();
        artifacts.add(Artifact.from("org.apache.camel.quarkus", "camel-quarkus-azure-key-vault"));
        addCapabilityAndDependecies(runtimeSpec, catalogSpec, "azure-key-vault", artifacts, false);

        artifacts.clear();
        artifacts.add(Artifact.from("org.apache.camel.quarkus", "camel-quarkus-aws-secrets-manager"));
        addCapabilityAndDependecies(runtimeSpec, catalogSpec, "aws-secrets-manager", artifacts, false);

        artifacts.clear();
        artifacts.add(Artifact.from("org.apache.camel.quarkus", "camel-quarkus-google-secret-manager"));
        addCapabilityAndDependecies(runtimeSpec, catalogSpec, "gcp-secret-manager", artifacts, false);

        artifacts.clear();
        artifacts.add(Artifact.from("org.apache.camel.k", "camel-k-knative-impl"));
        artifacts.add(Artifact.from("org.apache.camel.quarkus", "camel-quarkus-knative"));
        addCapabilityAndDependecies(runtimeSpec, catalogSpec, "knative", artifacts, false);

        artifacts.clear();
        artifacts.add(Artifact.from("io.micrometer", "micrometer-registry-prometheus"));
        artifacts.add(Artifact.from("org.apache.camel.quarkus", "camel-quarkus-micrometer"));
        addCapabilityAndDependecies(runtimeSpec, catalogSpec, "prometheus", artifacts, false);

        artifacts.clear();
        artifacts.add(Artifact.from("org.apache.camel.quarkus", "camel-quarkus-management"));
        artifacts.add(Artifact.from("org.apache.camel.quarkus", "camel-quarkus-jaxb"));
        artifacts.add(Artifact.from("org.jolokia", "jolokia-agent-jvm", "javaagent"));
        addCapabilityAndDependecies(runtimeSpec, catalogSpec, "jolokia", artifacts, true);

        artifacts.clear();
        properties.clear();
        properties.add(Property.from("quarkus.log.level", "${camel.k.logging.level}"));
        properties.add(Property.from("quarkus.console.color", "${camel.k.logging.color}"));
        properties.add(Property.from("quarkus.log.console.format", "${camel.k.logging.format}"));
        properties.add(Property.from("quarkus.log.console.json", "${camel.k.logging.json}"));
        properties.add(Property.from("quarkus.log.console.json.pretty-print", "${camel.k.logging.jsonPrettyPrint}"));
        addCapability(runtimeSpec, catalogSpec, "logging", artifacts, properties, new ArrayList<>(), new ArrayList<>(), false);

        artifacts.clear();
        properties.clear();
        properties.add(Property.from("quarkus.kubernetes-service-binding.enabled", "${camel.k.serviceBinding.enabled}"));
        addCapability(runtimeSpec, catalogSpec, "service-binding", artifacts, properties, new ArrayList<>(), new ArrayList<>(), false);
    }

    private void addCapabilityAndDependecies(RuntimeSpec.Builder runtimeSpec, CamelCatalogSpec.Builder catalogSpec, String name,
            List<Artifact> artifacts, boolean addDependency) {
        if (capabilitiesExclusionList != null && !capabilitiesExclusionList.contains(name)) {
            CamelCapability.Builder capBuilder = new CamelCapability.Builder();
            artifacts.forEach(artifact -> {
                capBuilder.addDependency(artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier());
                if (addDependency) {
                    catalogSpec.putArtifact(new CamelArtifact.Builder()
                        .groupId(artifact.getGroupId())
                        .artifactId(artifact.getArtifactId())
                        .classifier(artifact.getClassifier())
                        .build());
                }
            });
            CamelCapability dependency = capBuilder.build();
            runtimeSpec.putCapability(name, dependency);
        }
    }

    private void addCapability(RuntimeSpec.Builder runtimeSpec, CamelCatalogSpec.Builder catalogSpec, String name,
    List<Artifact> artifacts, List<Property> runtimeProperties, List<Property> buildTimeProperties, List<Property> metadataProperties, boolean addDependency) {
    if (capabilitiesExclusionList != null && !capabilitiesExclusionList.contains(name)) {
        CamelCapability.Builder capBuilder = new CamelCapability.Builder();
        runtimeProperties.forEach(property -> {
            capBuilder.addRuntimeProperty(property.getKey(), property.getValue());
        });
        buildTimeProperties.forEach(property -> {
            capBuilder.addBuildTimeProperty(property.getKey(), property.getValue());
        });
        metadataProperties.forEach(property -> {
            capBuilder.putMetadata(property.getKey(), property.getValue());
        });
        artifacts.forEach(artifact -> {
        capBuilder.addDependency(artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier());
        if (addDependency) {
            catalogSpec.putArtifact(new CamelArtifact.Builder()
                .groupId(artifact.getGroupId())
                .artifactId(artifact.getArtifactId())
                .classifier(artifact.getClassifier())
                .build());
        }
        });
        CamelCapability cap = capBuilder.build();
        runtimeSpec.putCapability(name, cap);
        }
    }
}

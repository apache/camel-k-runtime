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
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.k.tooling.maven.model.CamelArtifact;
import org.apache.camel.k.tooling.maven.model.CatalogComponentDefinition;
import org.apache.camel.k.tooling.maven.model.CatalogDataFormatDefinition;
import org.apache.camel.k.tooling.maven.model.CatalogLanguageDefinition;
import org.apache.camel.k.tooling.maven.model.CatalogProcessor;
import org.apache.camel.k.tooling.maven.model.CatalogSupport;
import org.apache.camel.k.tooling.maven.model.crd.CamelCatalog;
import org.apache.camel.k.tooling.maven.model.crd.CamelCatalogSpec;
import org.apache.camel.k.tooling.maven.model.crd.QuarkusRuntimeProvider;
import org.apache.camel.k.tooling.maven.model.crd.RuntimeProvider;
import org.apache.camel.k.tooling.maven.model.k8s.ObjectMeta;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

@Mojo(
    name = "generate-catalog",
    defaultPhase = LifecyclePhase.GENERATE_RESOURCES,
    threadSafe = true,
    requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
    requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class GenerateCatalogMojo extends AbstractMojo {

    @Parameter(readonly = true, defaultValue = "${project}")
    private MavenProject project;

    @Parameter(property = "catalog.path", defaultValue = "${project.build.directory}")
    private String outputPath;

    @Parameter(property = "catalog.file", defaultValue = "camel-catalog-${camel.version}-${runtime.version}.yaml")
    private String outputFile;

    @Parameter(property = "catalog.runtime", defaultValue = "")
    private String runtime;

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

        final SortedMap<String, CamelArtifact> artifacts = new TreeMap<>();
        final org.apache.camel.catalog.CamelCatalog catalog = new DefaultCamelCatalog();
        if (runtime == null) {
            runtime = "";
        }
        switch (runtime) {
        case "quarkus":
            catalog.setRuntimeProvider(new org.apache.camel.catalog.quarkus.QuarkusRuntimeProvider());
            break;
        case "":
            break;
        default:
            throw new IllegalArgumentException("catalog.runtime parameter value [" + runtime + "] is not supported!");
        }

        try {
            processComponents(catalog, artifacts);
            processLanguages(catalog, artifacts);
            processDataFormats(catalog, artifacts);

            ServiceLoader<CatalogProcessor> processors = ServiceLoader.load(CatalogProcessor.class);
            Comparator<CatalogProcessor> comparator = Comparator.comparingInt(CatalogProcessor::getOrder);

            //
            // post process catalog
            //
            StreamSupport.stream(processors.spliterator(), false).sorted(comparator).filter(p -> p.accepts(catalog)).forEach(p -> {
                getLog().info("Executing processor: " + p.getClass().getName());

                p.process(project, catalog, artifacts);
            });

            //
            // apiVersion: camel.apache.org/v1
            // kind: CamelCatalog
            // metadata:
            //   name: catalog-x.y.z-a.b.c
            //   labels:
            //     app: "camel-k"
            //     camel.apache.org/catalog.version: x.y.x
            //     camel.apache.org/catalog.loader.version: x.y.z
            //     camel.apache.org/runtime.version: x.y.x
            // spec:
            //   version:
            //   runtimeVersion:
            // status:
            //   artifacts:
            //
            try (Writer writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
                String catalogName;
                if ("quarkus".equals(runtime)) {
                    catalogName = String.format("camel-catalog-%s-%s-%s",
                        runtime,
                        getVersionFor("/META-INF/maven/org.apache.camel.quarkus/camel-catalog-quarkus/pom.properties").toLowerCase(),
                        getRuntimeVersion().toLowerCase()
                    );
                } else {
                    catalogName = String.format("camel-catalog-%s-%s",
                        catalog.getCatalogVersion().toLowerCase(),
                        getRuntimeVersion().toLowerCase()
                    );
                }

                ObjectMeta.Builder labels = new ObjectMeta.Builder()
                    .name(catalogName)
                    .putLabels("app", "camel-k")
                    .putLabels("camel.apache.org/catalog.version", catalog.getCatalogVersion())
                    .putLabels("camel.apache.org/catalog.loader.version", catalog.getLoadedVersion())
                    .putLabels("camel.apache.org/runtime.version", getRuntimeVersion());
                if (!"".equals(runtime)) {
                    labels.putLabels("camel.apache.org/runtime.provider", runtime);
                }

                CamelCatalogSpec.Builder catalogSpec = new CamelCatalogSpec.Builder()
                .version(catalog.getCatalogVersion())
                .runtimeVersion(getRuntimeVersion())
                .artifacts(artifacts);

                if ("quarkus".equals(runtime)) {
                    String camelQuarkusVersion = getVersionFor("/META-INF/maven/org.apache.camel.quarkus/camel-catalog-quarkus/pom.properties");
                    String quarkusVersion = getVersionFor("/META-INF/maven/io.quarkus/quarkus-core/pom.properties");
                    catalogSpec.runtimeProvider(new RuntimeProvider.Builder()
                    .quarkus(new QuarkusRuntimeProvider.Builder()
                        .camelQuarkusVersion(camelQuarkusVersion)
                        .quarkusVersion(quarkusVersion)
                        .build())
                    .build());
                }

                CamelCatalog cr = new CamelCatalog.Builder()
                    .metadata(labels.build())
                    .spec(catalogSpec.build())
                    .build();

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

    private void processComponents(org.apache.camel.catalog.CamelCatalog catalog, Map<String, CamelArtifact> artifacts) {
        for (String name : catalog.findComponentNames()) {
            String json = catalog.componentJSonSchema(name);

            if ("rest-swagger".equalsIgnoreCase(name)) {
                // TODO: workaround for https://issues.apache.org/jira/browse/CAMEL-13588
                json = json.replaceAll(Pattern.quote("\\h"), "h");
            }

            CatalogComponentDefinition definition = CatalogSupport.unmarshallComponent(json);

            artifacts.compute(definition.getArtifactId(), (key, artifact) -> {
                if (artifact == null) {
                    artifact = new CamelArtifact();
                    artifact.setGroupId(definition.getGroupId());
                    artifact.setArtifactId(definition.getArtifactId());

                    Objects.requireNonNull(artifact.getGroupId());
                    Objects.requireNonNull(artifact.getArtifactId());
                }

                definition.getSchemes()
                    .map(StringUtils::trimToNull)
                    .filter(Objects::nonNull)
                    .forEach(artifact::createScheme);

                artifact.addJavaType(definition.getJavaType());

                return artifact;
            });
        }
    }

    private void processLanguages(org.apache.camel.catalog.CamelCatalog catalog, Map<String, CamelArtifact> artifacts) {
        for (String name : catalog.findLanguageNames()) {
            String json = catalog.languageJSonSchema(name);
            CatalogLanguageDefinition definition = CatalogSupport.unmarshallLanguage(json);

            artifacts.compute(definition.getArtifactId(), (key, artifact) -> {
                if (artifact == null) {
                    artifact = new CamelArtifact();
                    artifact.setGroupId(definition.getGroupId());
                    artifact.setArtifactId(definition.getArtifactId());

                    Objects.requireNonNull(artifact.getGroupId());
                    Objects.requireNonNull(artifact.getArtifactId());
                }

                artifact.addLanguage(definition.getName());
                artifact.addJavaType(definition.getJavaType());

                return artifact;
            });
        }
    }

    private void processDataFormats(org.apache.camel.catalog.CamelCatalog catalog, Map<String, CamelArtifact> artifacts) {
        for (String name : catalog.findDataFormatNames()) {
            String json = catalog.dataFormatJSonSchema(name);
            CatalogDataFormatDefinition definition = CatalogSupport.unmarshallDataFormat(json);

            artifacts.compute(definition.getArtifactId(), (key, artifact) -> {
                if (artifact == null) {
                    artifact = new CamelArtifact();
                    artifact.setGroupId(definition.getGroupId());
                    artifact.setArtifactId(definition.getArtifactId());

                    Objects.requireNonNull(artifact.getGroupId());
                    Objects.requireNonNull(artifact.getArtifactId());
                }

                artifact.addDataformats(definition.getName());
                artifact.addJavaType(definition.getJavaType());

                return artifact;
            });
        }
    }

    private String getRuntimeVersion() {
        return getVersionFor("/META-INF/maven/org.apache.camel.k/camel-k-maven-plugin/pom.properties");
    }

    private synchronized String getVersionFor(String path) {
        String version = null;

        // try to load from maven properties first
        try {
            InputStream is = getClass().getResourceAsStream(path);

            if (is != null) {
                Properties p = new Properties();
                p.load(is);
                version = p.getProperty("version", "");
            }
        } catch (Exception e) {
            // ignore
        }

        // fallback to using Java API
        if (version == null) {
            Package aPackage = getClass().getPackage();
            if (aPackage != null) {
                version = aPackage.getImplementationVersion();
                if (version == null) {
                    version = aPackage.getSpecificationVersion();
                }
            }
        }

        if (version == null) {
            // we could not compute the version so use a blank
            throw new IllegalStateException("Unable to determine runtime version");
        }

        return version;
    }
}

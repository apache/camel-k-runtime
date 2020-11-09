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
import java.util.Comparator;
import java.util.Locale;
import java.util.ServiceLoader;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.catalog.quarkus.QuarkusRuntimeProvider;
import org.apache.camel.impl.engine.AbstractCamelContext;
import org.apache.camel.k.tooling.maven.model.CamelCapability;
import org.apache.camel.k.tooling.maven.model.CatalogProcessor;
import org.apache.camel.k.tooling.maven.model.crd.CamelCatalog;
import org.apache.camel.k.tooling.maven.model.crd.CamelCatalogSpec;
import org.apache.camel.k.tooling.maven.model.crd.RuntimeSpec;
import org.apache.camel.k.tooling.maven.model.k8s.ObjectMeta;
import org.apache.camel.k.tooling.maven.support.MavenSupport;
import org.apache.camel.quarkus.core.FastCamelContext;
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

    @Parameter(property = "catalog.file", defaultValue = "camel-catalog-${runtime.version}.yaml")
    private String outputFile;

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
            RuntimeSpec.Builder runtimeSpec = new RuntimeSpec.Builder()
                .version(runtimeVersion);

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

            runtimeSpec.applicationClass("io.quarkus.runner.GeneratedMain");
            runtimeSpec.addDependency("org.apache.camel.k", "camel-k-runtime");
            runtimeSpec.putCapability(
                "cron",
                CamelCapability.forArtifact(
                    "org.apache.camel.k", "camel-k-cron"));
            runtimeSpec.putCapability(
                "health",
                CamelCapability.forArtifact(
                    "org.apache.camel.quarkus", "camel-quarkus-microprofile-health"));
            runtimeSpec.putCapability(
                "platform-http",
                CamelCapability.forArtifact(
                    "org.apache.camel.quarkus", "camel-quarkus-platform-http"));
            runtimeSpec.putCapability(
                "rest",
                new CamelCapability.Builder()
                    .addDependency("org.apache.camel.quarkus", "camel-quarkus-rest")
                    .addDependency("org.apache.camel.quarkus", "camel-quarkus-platform-http")
                    .build());
            runtimeSpec.putCapability(
                "circuit-breaker",
                CamelCapability.forArtifact(
                    "org.apache.camel.quarkus", "camel-quarkus-microprofile-fault-tolerance"));
            runtimeSpec.putCapability(
                "tracing",
                CamelCapability.forArtifact(
                    "org.apache.camel.quarkus", "camel-quarkus-opentracing"));
            runtimeSpec.putCapability(
                "master",
                CamelCapability.forArtifact(
                    "org.apache.camel.k", "camel-k-master"));

            CamelCatalogSpec.Builder catalogSpec = new CamelCatalogSpec.Builder()
                .runtime(runtimeSpec.build());


            StreamSupport.stream(ServiceLoader.load(CatalogProcessor.class).spliterator(), false)
                .sorted(Comparator.comparingInt(CatalogProcessor::getOrder))
                .filter(p -> p.accepts(catalog))
                .forEach(p -> p.process(project, catalog, catalogSpec));

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
}

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
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Modifier;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.google.common.base.CaseFormat;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import org.apache.camel.util.AntPathMatcher;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;

@Mojo(
    name = "generate-yaml-loader-support-classes",
    inheritByDefault = false,
    defaultPhase = LifecyclePhase.GENERATE_SOURCES,
    requiresDependencyResolution = ResolutionScope.COMPILE,
    threadSafe = true,
    requiresProject = false)
public class GenerateYamlLoaderSupportClasses extends GenerateYamlSupport {
    @Parameter
    protected List<String> bannedDefinitions;
    @Parameter(defaultValue = "${project.build.directory}/generated-sources/camel")
    protected String output;

    @Override
    public void execute() throws MojoFailureException {
        try {
            JavaFile.builder("org.apache.camel.k.loader.yaml", generateJacksonModule())
                .indent("    ")
                .build()
                .writeTo(Paths.get(output));
            JavaFile.builder("org.apache.camel.k.loader.yaml", generateReifiers())
                .indent("    ")
                .build()
                .writeTo(Paths.get(output));
            JavaFile.builder("org.apache.camel.k.loader.yaml", generateResolver())
                .indent("    ")
                .build()
                .writeTo(Paths.get(output));
        } catch (IOException e) {
            throw new MojoFailureException(e.getMessage());
        }
    }

    public final TypeSpec generateJacksonModule() {
        TypeSpec.Builder type = TypeSpec.classBuilder("YamlModule");
        type.addModifiers(Modifier.PUBLIC, Modifier.FINAL);
        type.superclass(Module.class);
        type.addMethod(
            MethodSpec.methodBuilder("getModuleName")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(String.class)
                .addCode(CodeBlock.builder().addStatement("return $S", "camel-yaml").build())
                .build()
        );
        type.addMethod(
            MethodSpec.methodBuilder("version")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(Version.class)
                .addCode(CodeBlock.builder().addStatement("return $L", "Version.unknownVersion()").build())
                .build()
        );

        MethodSpec.Builder mb = MethodSpec.methodBuilder("setupModule")
            .addModifiers(Modifier.PUBLIC)
            .addParameter(Module.SetupContext.class, "context");

        definitions(EXPRESSION_DEFINITION_CLASS).forEach(
            (k, v) -> mb.addStatement("context.registerSubtypes(new com.fasterxml.jackson.databind.jsontype.NamedType($L.class, $S))", v.name().toString(), k)
        );
        definitions(DATAFORMAT_DEFINITION_CLASS).forEach(
            (k, v) -> mb.addStatement("context.registerSubtypes(new com.fasterxml.jackson.databind.jsontype.NamedType($L.class, $S))", v.name().toString(), k)
        );
        definitions(LOAD_BALANCE_DEFINITION_CLASS).forEach(
            (k, v) -> mb.addStatement("context.registerSubtypes(new com.fasterxml.jackson.databind.jsontype.NamedType($L.class, $S))", v.name().toString(), k)
        );

        annotated(YAML_MIXIN_ANNOTATION).forEach(i -> {
            final AnnotationInstance annotation = i.classAnnotation(YAML_MIXIN_ANNOTATION);
            final AnnotationValue targets = annotation.value("value");

            String name = i.toString();
            if (i.nestingType() == ClassInfo.NestingType.INNER) {
                name = i.enclosingClass().toString() + "." + i.simpleName();
            }

            if (targets != null) {
                for (String target: targets.asStringArray()) {
                    mb.addStatement("context.setMixInAnnotations($L.class, $L.class);", target, name);
                }
            }
        });


        type.addMethod(mb.build());

        return type.build();
    }

    public final TypeSpec generateReifiers() {
        TypeSpec.Builder type = TypeSpec.classBuilder("YamlReifiers");
        type.addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        MethodSpec.Builder mb = MethodSpec.methodBuilder("registerReifiers")
            .addModifiers(Modifier.PUBLIC)
            .addModifiers(Modifier.STATIC);

        annotated(YAML_NODE_DEFINITION_ANNOTATION).forEach(i -> {
            final AnnotationInstance annotation = i.classAnnotation(YAML_NODE_DEFINITION_ANNOTATION);
            final AnnotationValue reifiers = annotation.value("reifiers");

            if (reifiers != null) {
                String name = i.toString();
                if (i.nestingType() == ClassInfo.NestingType.INNER) {
                    name = i.enclosingClass().toString() + "." + i.simpleName();
                }

                for (String reifier: reifiers.asStringArray()) {
                    mb.addStatement("org.apache.camel.reifier.ProcessorReifier.registerReifier($L.class, $L::new)", name, reifier);
                }
            }
        });

        type.addMethod(mb.build());

        return type.build();
    }

    public final TypeSpec generateResolver() {
        Set<String> ids = new HashSet<>();

        MethodSpec.Builder mb = MethodSpec.methodBuilder("resolve")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .addParameter(ClassName.get("org.apache.camel", "CamelContext"), "camelContext")
            .addParameter(ClassName.get("java.lang", "String"), "id")
            .returns(ClassName.get("org.apache.camel.k.loader.yaml.spi", "StepParser"));

        mb.beginControlFlow("switch(id)");

        // custom parsers
        annotated(YAML_STEP_PARSER_ANNOTATION)
            .sorted(Comparator.comparing(i -> i.name().toString()))
            .forEach(
                i -> {
                    AnnotationValue id = i.classAnnotation(YAML_STEP_PARSER_ANNOTATION).value("id");
                    if (id != null) {
                        if (ids.add(id.asString())) {
                            mb.beginControlFlow("case $S:", id.asString());
                            mb.addStatement("return new $L()", i.name().toString());
                            mb.endControlFlow();
                        }
                    }

                    AnnotationValue aliases = i.classAnnotation(YAML_STEP_PARSER_ANNOTATION).value("aliases");
                    if (aliases != null) {
                        for (String alias : aliases.asStringArray()) {
                            if (ids.add(alias)) {
                                mb.beginControlFlow("case $S:", alias);
                                mb.addStatement("return new $L()", i.name().toString());
                                mb.endControlFlow();
                            }
                        }
                    }
                }
            );

        // auto generated parsers
        annotated(XML_ROOT_ELEMENT_ANNOTATION_CLASS)
            .forEach(
                i -> {
                    AnnotationInstance meta = i.classAnnotation(METADATA_ANNOTATION);
                    AnnotationInstance root = i.classAnnotation(XML_ROOT_ELEMENT_ANNOTATION_CLASS);

                    if (meta != null && root != null) {
                        AnnotationValue name = root.value("name");
                        AnnotationValue label = meta.value("label");

                        if (name != null && label != null) {
                            if (bannedDefinitions != null) {
                                for (String bannedDefinition: bannedDefinitions) {
                                    if (AntPathMatcher.INSTANCE.match(bannedDefinition.replace('.', '/'), i.name().toString('/'))) {
                                        getLog().debug("Skipping definition: " + i.name().toString());
                                        return;
                                    }
                                }
                            }

                            Set<String> labels = Set.of(label.asString().split(",", -1));
                            if (labels.contains("eip")) {
                                String id = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, name.asString());
                                if (ids.add(id)) {
                                    mb.beginControlFlow("case $S:", id);
                                    mb.addStatement("return new org.apache.camel.k.loader.yaml.parser.TypedProcessorStepParser($L.class)", i.name().toString());
                                    mb.endControlFlow();
                                }
                            }
                        }
                    }
                }
            );

        mb.beginControlFlow("default:");
        mb.addStatement("return lookup(camelContext, id)");
        mb.endControlFlow();
        mb.endControlFlow();

        return TypeSpec.classBuilder("YamlStepResolver")
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addSuperinterface(ClassName.get("org.apache.camel.k.loader.yaml.spi", "StepParser.Resolver"))
            .addMethod(mb.build())
            .build();
    }
}

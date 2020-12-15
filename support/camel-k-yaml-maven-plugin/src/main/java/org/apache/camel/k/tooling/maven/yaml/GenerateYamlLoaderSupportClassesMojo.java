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
package org.apache.camel.k.tooling.maven.yaml;


import java.io.IOException;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import javax.lang.model.element.Modifier;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.util.StringHelper;
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
public class GenerateYamlLoaderSupportClassesMojo extends GenerateYamlSupportMojo {
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
            throw new MojoFailureException(e.getMessage(), e);
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

        elementsOf(EXPRESSION_DEFINITION_CLASS)
            .forEach( (k, v) -> registerSubtypes(mb, k, v));
        elementsOf(DATAFORMAT_DEFINITION_CLASS)
            .forEach( (k, v) -> registerSubtypes(mb, k, v));
        elementsOf(LOAD_BALANCE_DEFINITION_CLASS)
            .forEach( (k, v) -> registerSubtypes(mb, k, v));

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


        if (deserializers != null) {
            deserializers.forEach((k,v ) -> {
                mb.addStatement("deserializers.addDeserializer($L.class, new $L())", k, v);
            });
        }

        mb.addStatement("var deserializers = new com.fasterxml.jackson.databind.module.SimpleDeserializers()");
        annotated(YAML_DESERIALIZER_ANNOTATION).forEach(i -> {
            final AnnotationInstance annotation = i.classAnnotation(YAML_DESERIALIZER_ANNOTATION);
            final String annotationValue = annotation.value("value").asString();

            if (annotationValue != null) {
                if (deserializers != null && deserializers.containsKey(annotationValue)) {
                    return;
                }

                String deserializer = i.toString();
                if (i.nestingType() == ClassInfo.NestingType.INNER) {
                    deserializer = i.enclosingClass().toString() + "." + i.simpleName();
                }

                mb.addStatement("deserializers.addDeserializer($L.class, new $L())", annotationValue, deserializer);
            }
        });

        mb.addStatement("context.addDeserializers(deserializers)");


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

        mb.addComment("custom parsers");

        annotated(YAML_STEP_PARSER_ANNOTATION)
            .sorted(Comparator.comparing(i -> i.name().toString()))
            .forEach(
                i -> {
                    AnnotationValue id = i.classAnnotation(YAML_STEP_PARSER_ANNOTATION).value("id");
                    if (id != null && ids.add(id.asString())) {
                        mb.beginControlFlow("case $S:", id.asString());
                        mb.addStatement("return new $L()", i.name().toString());
                        mb.endControlFlow();
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

        mb.addComment("auto generated parsers");

        for (var entry: models().entrySet()) {
            final String typeName = entry.getValue().name().toString();
            final String stepId = StringHelper.camelCaseToDash(entry.getKey());

            if (ids.add(stepId)) {
                mb.beginControlFlow("case $S:", stepId);
                mb.addStatement("return new org.apache.camel.k.loader.yaml.parser.TypedProcessorStepParser($L.class)", typeName);
                mb.endControlFlow();
            }
        }

        mb.addComment("endpoint dsl");

        CamelCatalog catalog = new DefaultCamelCatalog();
        catalog.findComponentNames().stream()
            .sorted()
            .map(catalog::componentModel)
            .flatMap(component -> combine(component.getScheme(), component.getAlternativeSchemes()))
            .filter(ids::add)
            .forEach(scheme -> {
                mb.beginControlFlow("case $S:", scheme);
                mb.addStatement("return new org.apache.camel.k.loader.yaml.parser.EndpointStepParser($S)", scheme);
                mb.endControlFlow();
            });

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

    private static void registerSubtypes(MethodSpec.Builder mb, String id, ClassInfo info) {
        final String name = info.name().toString();
        final String stm = "context.registerSubtypes(new com.fasterxml.jackson.databind.jsontype.NamedType($L.class, $S))";

        mb.addStatement(stm, name, id);
        if (!id.equals(StringHelper.camelCaseToDash(id))) {
            mb.addStatement(stm, name, StringHelper.camelCaseToDash(id));
        }
    }
}

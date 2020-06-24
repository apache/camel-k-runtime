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

import javax.lang.model.element.Modifier;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import org.apache.camel.model.DataFormatDefinition;
import org.apache.camel.model.LoadBalancerDefinition;
import org.apache.camel.model.language.ExpressionDefinition;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(
    name = "generate-yaml-parser-support-classes",
    inheritByDefault = false,
    defaultPhase = LifecyclePhase.GENERATE_SOURCES,
    requiresDependencyResolution = ResolutionScope.COMPILE,
    threadSafe = true)
public class GenerateYamlParserSupportClasses extends GenerateYamlSupport {
    @Parameter(defaultValue = "${project.build.directory}/generated-sources/camel")
    protected String output;

    @Override
    public void execute() throws MojoFailureException {
        try {
            JavaFile.builder("org.apache.camel.k.loader.yaml.parser", generateHasExpression())
                .indent("    ")
                .build()
                .writeTo(Paths.get(output));
            JavaFile.builder("org.apache.camel.k.loader.yaml.parser", generateHasDataFormat())
                .indent("    ")
                .build()
                .writeTo(Paths.get(output));
            JavaFile.builder("org.apache.camel.k.loader.yaml.parser", generateHasLoadBalancerType())
                .indent("    ")
                .build()
                .writeTo(Paths.get(output));
        } catch (IOException e) {
            throw new MojoFailureException(e.getMessage());
        }
    }

    public final TypeSpec generateHasExpression() {
        TypeSpec.Builder type = TypeSpec.interfaceBuilder("HasExpression");
        type.addModifiers(Modifier.PUBLIC);
        type.addMethod(
            MethodSpec.methodBuilder("setExpression")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(ExpressionDefinition.class, "expressionDefinition")
                .addAnnotation(
                    AnnotationSpec.builder(JsonTypeInfo.class)
                        .addMember("use", "$L", "JsonTypeInfo.Id.NAME")
                        .addMember("include", "$L", "JsonTypeInfo.As.WRAPPER_OBJECT")
                        .build())
                .build()
        );

        type.addMethod(
            MethodSpec.methodBuilder("getExpression")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(ExpressionDefinition.class)
                .build()
        );

        definitions(EXPRESSION_DEFINITION_CLASS).forEach(
            (k, v) -> {
                String name = k;
                name = WordUtils.capitalize(name, '_', '-');
                name = StringUtils.remove(name, "_");
                name = StringUtils.remove(name, "-");

                type.addMethod(MethodSpec.methodBuilder("set" + name)
                    .addAnnotation(
                        AnnotationSpec.builder(JsonAlias.class).addMember("value", "$S", k).build())
                    .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                    .addParameter(loadClass(v), "definition")
                    .addCode(
                        CodeBlock.builder()
                            .beginControlFlow("if (getExpression() != null)")
                            .addStatement("throw new IllegalArgumentException(\"And expression has already been set\")")
                            .endControlFlow()
                            .addStatement("setExpression(definition);").build())
                    .build()
                );
            }
        );

        return type.build();
    }

    public final TypeSpec generateHasDataFormat() {
        TypeSpec.Builder type = TypeSpec.interfaceBuilder("HasDataFormat");
        type.addModifiers(Modifier.PUBLIC);
        type.addMethod(
            MethodSpec.methodBuilder("setDataFormatType")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(DataFormatDefinition.class, "dataFormatType")
                .addAnnotation(
                    AnnotationSpec.builder(JsonAlias.class).
                        addMember("value", "{$S, $S}", "data-format-type", "data-format")
                        .build())
                .addAnnotation(
                    AnnotationSpec.builder(JsonTypeInfo.class)
                        .addMember("use", "$L", "JsonTypeInfo.Id.NAME")
                        .addMember("include", "$L", "JsonTypeInfo.As.WRAPPER_OBJECT")
                        .build())
                .build()
        );

        type.addMethod(
            MethodSpec.methodBuilder("getDataFormatType")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(DataFormatDefinition.class)
                .build()
        );

        definitions(DATAFORMAT_DEFINITION_CLASS).forEach(
            (k, v) -> {
                String name = k;
                name = WordUtils.capitalize(name, '_', '-');
                name = StringUtils.remove(name, "_");
                name = StringUtils.remove(name, "-");

                type.addMethod(MethodSpec.methodBuilder("set" + name)
                    .addAnnotation(
                        AnnotationSpec.builder(JsonAlias.class).addMember("value", "$S", k).build())
                    .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                    .addParameter(loadClass(v), "definition")
                    .addCode(
                        CodeBlock.builder()
                            .beginControlFlow("if (getDataFormatType() != null)")
                            .addStatement("throw new IllegalArgumentException(\"A data format has already been set\")")
                            .endControlFlow()
                            .addStatement("setDataFormatType(definition);")
                            .build())
                    .build()
                );
            }
        );

        return type.build();
    }

    public final TypeSpec generateHasLoadBalancerType() {
        TypeSpec.Builder type = TypeSpec.interfaceBuilder("HasLoadBalancerType");
        type.addModifiers(Modifier.PUBLIC);
        type.addMethod(
            MethodSpec.methodBuilder("setLoadBalancerType")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(LoadBalancerDefinition.class, "loadbalancer")
                .addAnnotation(
                    AnnotationSpec.builder(JsonTypeInfo.class)
                        .addMember("use", "$L", "JsonTypeInfo.Id.NAME")
                        .addMember("include", "$L", "JsonTypeInfo.As.WRAPPER_OBJECT")
                        .build())
                .build()
        );

        type.addMethod(
            MethodSpec.methodBuilder("getLoadBalancerType")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(LoadBalancerDefinition.class)
                .build()
        );

        definitions(LOAD_BALANCE_DEFINITION_CLASS).forEach(
            (k, v) -> {
                String name = k;
                name = WordUtils.capitalize(name, '_', '-');
                name = StringUtils.remove(name, "_");
                name = StringUtils.remove(name, "-");

                type.addMethod(MethodSpec.methodBuilder("set" + name)
                    .addAnnotation(
                        AnnotationSpec.builder(JsonAlias.class).addMember("value", "$S", k).build())
                    .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                    .addParameter(loadClass(v), "definition")
                    .addCode(
                        CodeBlock.builder()
                            .beginControlFlow("if (getLoadBalancerType() != null)")
                            .addStatement("throw new IllegalArgumentException(\"A load-balancer has already been set\")")
                            .endControlFlow()
                            .addStatement("setLoadBalancerType(definition);").build())
                    .build()
                );
            }
        );

        return type.build();
    }
}

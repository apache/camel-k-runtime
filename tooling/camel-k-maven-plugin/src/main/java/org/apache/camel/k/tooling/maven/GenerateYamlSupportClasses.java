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


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Modifier;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import org.apache.camel.model.DataFormatDefinition;
import org.apache.camel.model.language.ExpressionDefinition;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexReader;
import org.jboss.jandex.IndexView;

@Mojo(
    name = "generate-yaml-support-classes",
    inheritByDefault = false,
    defaultPhase = LifecyclePhase.GENERATE_SOURCES,
    requiresDependencyResolution = ResolutionScope.COMPILE,
    threadSafe = true,
    requiresProject = false)
public class GenerateYamlSupportClasses extends AbstractMojo {
    public static final DotName EXPRESSION_DEFINITION_CLASS = DotName.createSimple("org.apache.camel.model.language.ExpressionDefinition");
    public static final DotName DATAFORMAT_DEFINITION_CLASS = DotName.createSimple("org.apache.camel.model.DataFormatDefinition");
    public static final DotName XMLROOTELEMENT_ANNOTATION_CLASS = DotName.createSimple("javax.xml.bind.annotation.XmlRootElement");

    @Parameter(readonly = true)
    private MavenProject project;
    @Parameter(defaultValue = "${project.build.directory}/generated-sources/camel")
    private String output;

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
            JavaFile.builder("org.apache.camel.k.loader.yaml", generateJacksonModule())
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
                    .addParameter(v, "definition")
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
                    .addParameter(v, "definition")
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
            (k, v) -> mb.addStatement("context.registerSubtypes(new com.fasterxml.jackson.databind.jsontype.NamedType($T.class, $S))", v, k)
        );
        definitions(DATAFORMAT_DEFINITION_CLASS).forEach(
            (k, v) -> mb.addStatement("context.registerSubtypes(new com.fasterxml.jackson.databind.jsontype.NamedType($T.class, $S))", v, k)
        );

        type.addMethod(mb.build());

        return type.build();
    }

    private Map<String, Class<?>> definitions(DotName type) {
        ClassLoader cl = getClassLoader();
        Map<String, Class<?>> definitions = new HashMap<>();
        IndexView view = getCompositeIndexer(cl);

        for (ClassInfo ci: view.getAllKnownSubclasses(type)) {
            AnnotationInstance instance = ci.classAnnotation(XMLROOTELEMENT_ANNOTATION_CLASS);
            if (instance != null) {
                AnnotationValue name = instance.value("name");
                if (name != null) {
                    try {
                        definitions.put(name.asString(), cl.loadClass(ci.name().toString()));
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        return Collections.unmodifiableMap(definitions);
    }

    private static IndexView getCompositeIndexer(ClassLoader classLoader) {
        try {
            Enumeration<URL> elements = classLoader.getResources("META-INF/jandex.idx");
            List<IndexView> allIndex = new ArrayList<>();

            for (Enumeration<URL> e = elements; e.hasMoreElements();) {
                try (InputStream is = e.nextElement().openStream()) {
                    allIndex.add(new IndexReader(is).read());
                }
            }

            return CompositeIndex.create(allIndex);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ClassLoader getClassLoader() {
        if (project == null) {
            return getClass().getClassLoader();
        }

        try {
            List<String> elements = new ArrayList<>();
            elements.addAll(project.getCompileClasspathElements());
            elements.addAll(project.getRuntimeClasspathElements());

            URL urls[] = new URL[elements.size()];
            for (int i = 0; i < elements.size(); ++i) {
                urls[i] = new File(elements.get(i)).toURI().toURL();
            }
            return new URLClassLoader(urls, this.getClass().getClassLoader());
        } catch (Exception e) {
            return this.getClass().getClassLoader();
        }
    }
}

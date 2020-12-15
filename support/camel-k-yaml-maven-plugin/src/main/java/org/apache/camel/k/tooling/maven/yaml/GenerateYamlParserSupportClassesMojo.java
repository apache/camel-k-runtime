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
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.lang.model.element.Modifier;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.JsonNode;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.model.DataFormatDefinition;
import org.apache.camel.model.language.ExpressionDefinition;
import org.apache.camel.util.StringHelper;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;

@Mojo(
    name = "generate-yaml-parser-support-classes",
    inheritByDefault = false,
    defaultPhase = LifecyclePhase.GENERATE_SOURCES,
    requiresDependencyResolution = ResolutionScope.COMPILE,
    threadSafe = true)
public class GenerateYamlParserSupportClassesMojo extends GenerateYamlSupportMojo {
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
            JavaFile.builder("org.apache.camel.k.loader.yaml.parser", generateHasEndpointConsumer())
                .indent("    ")
                .build()
                .writeTo(Paths.get(output));
            JavaFile.builder("org.apache.camel.k.loader.yaml.parser", generateHasEndpointProducer())
                .indent("    ")
                .build()
                .writeTo(Paths.get(output));
            JavaFile.builder("org.apache.camel.k.loader.yaml.parser", generateDataFormatDeserializers())
                .indent("    ")
                .build()
                .writeTo(Paths.get(output));
            JavaFile.builder("org.apache.camel.k.loader.yaml.parser", generateExpressionDeserializers())
                .indent("    ")
                .build()
                .writeTo(Paths.get(output));
            JavaFile.builder("org.apache.camel.k.loader.yaml.parser", generateModelDeserializers())
                .indent("    ")
                .build()
                .writeTo(Paths.get(output));
        } catch (Exception e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
    }

    // ********************************
    //
    // Helpers
    //
    // ********************************

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

        elementsOf(EXPRESSION_DEFINITION_CLASS).forEach(
            (k, v) -> {
                String name = k;
                name = StringHelper.capitalize(name);
                name = StringHelper.replaceAll(name, "_", "");
                name = StringHelper.replaceAll(name, "-", "");

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
                            .addStatement("setExpression(definition)")
                            .build())
                    .build()
                );
            }
        );

        MethodSpec.Builder get = MethodSpec.methodBuilder("getExpressionType")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addParameter(ObjectCodec.class, "codec")
            .addParameter(JsonNode.class, "node")
            .addException(IOException.class)
            .returns(ExpressionDefinition.class);

        get.addStatement("JsonNode expression = null");

        elementsOf(EXPRESSION_DEFINITION_CLASS).forEach(
            (k, v) -> {
                get.beginControlFlow("if ((expression = node.get($S)) != null)", k);
                get.addStatement("return codec.treeToValue(expression, $L.class)", v.name().toString());
                get.endControlFlow();
                if (!k.equals(StringHelper.camelCaseToDash(k))) {
                    get.beginControlFlow("if ((expression = node.get($S)) != null)", StringHelper.camelCaseToDash(k));
                    get.addStatement("return codec.treeToValue(expression, $L.class)", v.name().toString());
                    get.endControlFlow();
                }
            }
        );

        get.beginControlFlow("if ((expression = node.get($S)) != null)", "expression");
        get.addStatement("return getExpressionType(codec, expression)");
        get.endControlFlow();

        get.addStatement("return null");

        type.addMethod(get.build());

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

        elementsOf(DATAFORMAT_DEFINITION_CLASS).forEach(
            (k, v) -> {
                String name = k;
                name = StringHelper.capitalize(name);
                name = StringHelper.replaceAll(name, "_", "");
                name = StringHelper.replaceAll(name, "-", "");

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
                            .addStatement("setDataFormatType(definition)")
                            .build())
                    .build()
                );
            }
        );

        MethodSpec.Builder get = MethodSpec.methodBuilder("getDataFormatType")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addParameter(ObjectCodec.class, "codec")
            .addParameter(JsonNode.class, "node")
            .addException(IOException.class)
            .returns(DataFormatDefinition.class);

        get.addStatement("JsonNode dataformat = null");

        elementsOf(DATAFORMAT_DEFINITION_CLASS).forEach(
            (k, v) -> {
                get.beginControlFlow("if ((dataformat = node.get($S)) != null)", k);
                get.addStatement("return codec.treeToValue(dataformat, $L.class)", v.name().toString());
                get.endControlFlow();
                if (!k.equals(StringHelper.camelCaseToDash(k))) {
                    get.beginControlFlow("if ((dataformat = node.get($S)) != null)", StringHelper.camelCaseToDash(k));
                    get.addStatement("return codec.treeToValue(dataformat, $L.class)", v.name().toString());
                    get.endControlFlow();
                }
            }
        );

        get.beginControlFlow("if ((dataformat = node.get($S)) != null)", "data-format");
        get.addStatement("return getDataFormatType(codec, dataformat)");
        get.endControlFlow();
        get.beginControlFlow("if ((dataformat = node.get($S)) != null)", "data-format-type");
        get.addStatement("return getDataFormatType(codec, dataformat)");
        get.endControlFlow();

        get.addStatement("return null");

        type.addMethod(get.build());

        return type.build();
    }

    public final TypeSpec generateHasEndpointConsumer() {
        TypeSpec.Builder type = TypeSpec.interfaceBuilder("HasEndpointConsumer");
        type.addModifiers(Modifier.PUBLIC);
        type.addSuperinterface(ClassName.get("org.apache.camel.k.loader.yaml.spi", "HasEndpoint"));

        CamelCatalog catalog = new DefaultCamelCatalog();
        catalog.findComponentNames().stream()
            .map(catalog::componentModel)
            .filter(component -> !component.isProducerOnly())
            .flatMap(component -> combine(component.getScheme(), component.getAlternativeSchemes()))
            .sorted()
            .distinct()
            .forEach(scheme -> generateHasEndpointProducer(scheme, type));

        return type.build();
    }

    public final TypeSpec generateHasEndpointProducer() {
        TypeSpec.Builder type = TypeSpec.interfaceBuilder("HasEndpointProducer");
        type.addModifiers(Modifier.PUBLIC);
        type.addSuperinterface(ClassName.get("org.apache.camel.k.loader.yaml.spi", "HasEndpoint"));

        CamelCatalog catalog = new DefaultCamelCatalog();
        catalog.findComponentNames().stream()
            .map(catalog::componentModel)
            .filter(component -> !component.isConsumerOnly())
            .flatMap(component -> combine(component.getScheme(), component.getAlternativeSchemes()))
            .sorted()
            .distinct()
            .forEach(scheme -> generateHasEndpointProducer(scheme, type));

        return type.build();
    }

    private static void generateHasEndpointProducer(String scheme, TypeSpec.Builder type) {
        String name = StringHelper.dashToCamelCase(scheme);
        name = name.replaceAll("[^a-zA-Z0-9]", "");
        name = name.toLowerCase();

        type.addMethod(MethodSpec.methodBuilder("set_" + name)
            .addAnnotation(AnnotationSpec.builder(JsonProperty.class).addMember("value", "$S", scheme).build())
            .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
            .addParameter(ParameterizedTypeName.get(Map.class, String.class, Object.class), "parameters")
            .addCode(
                CodeBlock.builder()
                    .beginControlFlow("if (getEndpointScheme() != null)")
                    .addStatement("throw new IllegalArgumentException(\"And endpoint has already been set\")")
                    .endControlFlow()
                    .addStatement("setEndpointScheme($S);", scheme)
                    .addStatement("setParameters(parameters)")
                    .build())
            .build()
        );
    }

    // ********************************
    //
    // Deserializers
    //
    // ********************************

    private TypeSpec generateDataFormatDeserializers() throws Exception {
        return generateDeserializers("DataFormatDeserializers", elementsOf(DATAFORMAT_DEFINITION_CLASS).values());
    }

    private TypeSpec generateExpressionDeserializers() throws Exception {
        return generateDeserializers("ExpressionDeserializers", elementsOf(EXPRESSION_DEFINITION_CLASS).values());
    }

    private TypeSpec generateModelDeserializers() throws Exception {
        return generateDeserializers("ModelDeserializers", definitions());
    }

    private TypeSpec generateDeserializers(String name, Collection<ClassInfo> infos) throws Exception {
        final Set<String> existing = annotated(YAML_DESERIALIZER_ANNOTATION)
            .map(d -> annotationValue(d, YAML_DESERIALIZER_ANNOTATION, "value").orElse(null))
            .filter(Objects::nonNull)
            .map(AnnotationValue::asString)
            .collect(Collectors.toSet());

        if (deserializers != null) {
            existing.addAll(deserializers.keySet());
        }

        return buildType(
            name,
            infos.stream()
                .filter(d -> !existing.contains(d.name().toString()))
                .map(this::generateParser)
                .collect(Collectors.toList())
        );
    }

    // ********************************
    //
    // Helpers
    //
    // ********************************

    private TypeSpec generateParser(ClassInfo info) {
        final ClassName targetType = ClassName.get(info.name().prefix().toString(), info.name().withoutPackagePrefix());
        final ClassName serdeSupport = ClassName.get("org.apache.camel.k.loader.yaml.support.serde", "DeserializerSupport");
        final ClassName serdeAnnotation = ClassName.get("org.apache.camel.k.annotation.yaml", "YAMLDeserializer");

        TypeSpec.Builder type = TypeSpec.classBuilder(info.simpleName() + "Deserializer");
        type.addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);
        type.addAnnotation(AnnotationSpec.builder(serdeAnnotation).addMember("value", "$L.class", info.name().toString()).build());
        type.superclass(ParameterizedTypeName.get(serdeSupport, targetType));

        //
        // Constructors
        //
        type.addMethod(MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC)
            .addStatement("super($L.class)", info.simpleName())
            .build());

        //
        // T handledTypeInstance();
        //
        type.addMethod(MethodSpec.methodBuilder("handledTypeInstance")
            .addAnnotation(AnnotationSpec.builder(Override.class).build())
            .addModifiers(Modifier.PROTECTED)
            .returns(targetType)
            .addCode(
                CodeBlock.builder()
                    .addStatement("return new $L()", info.simpleName())
                    .build())
            .build());

        //
        // T handledTypeInstance(String value);
        //
        for (MethodInfo ctor: info.constructors()) {
            if (ctor.parameters().size() == 1 && ctor.parameters().get(0).name().equals(GenerateYamlSupportMojo.STRING_CLASS)) {
                if ((ctor.flags() & java.lang.reflect.Modifier.PUBLIC) == 0) {
                    break;
                }

                type.addMethod(MethodSpec.methodBuilder("handledTypeInstance")
                    .addAnnotation(AnnotationSpec.builder(Override.class).build())
                    .addModifiers(Modifier.PROTECTED)
                    .addParameter(String.class, "value")
                    .returns(targetType)
                    .addCode(
                        CodeBlock.builder()
                            .addStatement("return new $L(value)", info.simpleName())
                            .build())
                    .build());
                break;
            }
        }

        //
        // void setProperties(JsonParser parser, T target, JsonNode node)
        //
        for (FieldInfo field : fields(info)) {
            if (hasAnnotation(field, XML_ELEMENT_REF_ANNOTATION_CLASS) && field.type().name().equals(EXPRESSION_DEFINITION_CLASS)) {
                type.addMethod(MethodSpec.methodBuilder("setProperties")
                    .addAnnotation(AnnotationSpec.builder(Override.class).build())
                    .addModifiers(Modifier.PROTECTED)
                    .addParameter(JsonParser.class, "parser")
                    .addParameter(targetType, "target")
                    .addParameter(JsonNode.class, "node")
                    .addException(Exception.class)
                    .addCode(
                        CodeBlock.builder()
                            .addStatement("var ed = HasExpression.getExpressionType(parser.getCodec(), node)")
                            .beginControlFlow("if (ed != null)")
                            .addStatement("target.set$L(ed)", StringHelper.capitalize(field.name()))
                            .endControlFlow()
                            .addStatement("super.setProperties(parser, target, node)")
                            .build())
                    .build());
                break;
            }
        }

        //
        // void setProperty(JsonParser parser, T target, String propertyKey, String propertyName, JsonNode node)
        //
        type.addMethod(MethodSpec.methodBuilder("setProperty")
            .addAnnotation(AnnotationSpec.builder(Override.class).build())
            .addModifiers(Modifier.PROTECTED)
            .addParameter(JsonParser.class, "parser")
            .addParameter(targetType, "target")
            .addParameter(String.class, "propertyKey")
            .addParameter(String.class, "propertyName")
            .addParameter(JsonNode.class, "node")
            .addException(Exception.class)
            .addCode(generateSetValue(info))
            .build());

        return type.build();
    }

    private CodeBlock generateSetValue(ClassInfo info) {
        CodeBlock.Builder cb = CodeBlock.builder();
        cb.beginControlFlow("switch(propertyKey)");

        for (FieldInfo field : fields(info)) {
            generateSetValue(cb, field);
        }

        cb.endControlFlow();

        return cb.build();
    }

    private void generateSetValue(CodeBlock.Builder cb, FieldInfo field) {
        if(hasAnnotation(field, XML_TRANSIENT_CLASS)) {
            return;
        }

        //
        // XmlElements
        //
        if (hasAnnotation(field, XML_ELEMENTS_ANNOTATION_CLASS)) {
            AnnotationInstance[] elements = field.annotation(XML_ELEMENTS_ANNOTATION_CLASS).value().asNestedArray();

            if (elements.length > 1) {
                //TODO: org.apache.camel.model.cloud.ServiceCallExpressionConfiguration#expressionConfiguration is
                //      wrongly defined and need to be fixed
                cb.beginControlFlow("case $S:", StringHelper.camelCaseToDash(field.name()).toLowerCase(Locale.US));
                cb.addStatement("setProperties(parser, target, node)");
                cb.addStatement("break");
                cb.endControlFlow();
            }

            for (AnnotationInstance element: elements) {
                AnnotationValue name = element.value("name");
                AnnotationValue type = element.value("type");

                if (name != null && type != null) {
                    cb.beginControlFlow("case $S:", StringHelper.camelCaseToDash(name.asString()).toLowerCase(Locale.US));
                    cb.addStatement("target.set$L(asType(parser, node, $L.class))", StringHelper.capitalize(field.name()), type.asString());
                    cb.addStatement("break");
                    cb.endControlFlow();
                }
            }

            return;
        }

        if (!hasAnnotation(field, XML_ATTRIBUTE_ANNOTATION_CLASS) &&
            !hasAnnotation(field, XML_VALUE_ANNOTATION_CLASS) &&
            !hasAnnotation(field, XML_ELEMENT_ANNOTATION_CLASS)) {
            return;
        }

        //
        // Others
        //
        cb.beginControlFlow("case $S:", StringHelper.camelCaseToDash(field.name()).toLowerCase(Locale.US));

        if (field.type().kind() == Type.Kind.PARAMETERIZED_TYPE) {
            ParameterizedType parameterized = field.type().asParameterizedType();

            if (!parameterized.name().equals(CLASS_CLASS) && parameterized.arguments().size() == 1) {
                final Type parametrizedType = parameterized.arguments().get(0);

                switch (parameterized.name().toString()) {
                    case "java.util.List":
                        if (parametrizedType.name().equals(STRING_CLASS)) {
                            cb.addStatement("target.set$L(asStringList(node.asText()))", StringHelper.capitalize(field.name()));
                            cb.addStatement("break");
                        } else {
                            cb.addStatement("target.set$L(asList(parser, node, $L.class))", StringHelper.capitalize(field.name()), parametrizedType.name().toString());
                            cb.addStatement("break");
                        }
                        cb.endControlFlow();
                        return;
                    case "java.util.Set":
                        if (parametrizedType.name().equals(STRING_CLASS)) {
                            cb.addStatement("target.set$L(asStringSet(node.asText()))", StringHelper.capitalize(field.name()));
                            cb.addStatement("break");
                        } else {
                            cb.addStatement("target.set$L(asSet(parser, node, $L.class))", StringHelper.capitalize(field.name()), parametrizedType.name().toString());
                            cb.addStatement("break");
                        }
                        cb.endControlFlow();
                        return;
                    default:
                        throw new UnsupportedOperationException("Unable to handle field: " + field.name() + " with type: " + field.type().name());
                }
            }
        }

        ClassInfo c = view.get().getClassByName(field.type().name());
        if (c != null && c.isEnum()) {
            cb.addStatement("target.set$L($L.valueOf(node.asText()))", StringHelper.capitalize(field.name()), field.type().name().toString());
            cb.addStatement("break");
        } else {
            switch (field.type().name().toString()) {
                case "[B":
                    cb.addStatement("target.set$L(asByteArray(node.asText()))", StringHelper.capitalize(field.name()));
                    cb.addStatement("break");
                    break;
                case "Z":
                    cb.addStatement("target.set$L(node.asBoolean())", StringHelper.capitalize(field.name()));
                    cb.addStatement("break");
                    break;
                case "I":
                    cb.addStatement("target.set$L(node.asInt())", StringHelper.capitalize(field.name()));
                    cb.addStatement("break");
                    break;
                case "J":
                    cb.addStatement("target.set$L(node.asLong())", StringHelper.capitalize(field.name()));
                    cb.addStatement("break");
                    break;
                case "D":
                    cb.addStatement("target.set$L(node.asDouble())", StringHelper.capitalize(field.name()));
                    cb.addStatement("break");
                    break;
                case "java.lang.String":
                    cb.addStatement("target.set$L(node.asText())", StringHelper.capitalize(field.name()));
                    cb.addStatement("break");
                    break;
                case "java.lang.Class":
                    cb.addStatement("target.set$L(asClass(node.asText()))", StringHelper.capitalize(field.name()));
                    cb.addStatement("break");
                    break;
                case "[Ljava.lang.Class;":
                    cb.addStatement("target.set$L(asClassArray(node.asText()))", StringHelper.capitalize(field.name()));
                    cb.addStatement("break");
                    break;
                case "java.lang.Integer":
                case "java.lang.Long":
                case "java.lang.Float":
                case "java.lang.Double":
                case "java.lang.Boolean":
                    cb.addStatement("target.set$L($L.valueOf(node.asText()))", StringHelper.capitalize(field.name()), field.type().name().toString());
                    cb.addStatement("break");
                    break;
                default:
                    if (field.type().kind() == Type.Kind.CLASS) {
                        cb.addStatement("target.set$L(asType(parser, node, $L.class))", StringHelper.capitalize(field.name()), field.type().name().toString());
                        cb.addStatement("break");
                    } else {
                        throw new UnsupportedOperationException("Unable to handle field: " + field.name() + " with type: " + field.type().name());
                    }
            }
        }

        cb.endControlFlow();
    }

    private TypeSpec buildType(String name, Collection<TypeSpec> types) throws Exception {
        TypeSpec.Builder type = TypeSpec.classBuilder(name);
        type.addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        types.forEach(type::addType);

        return type.build();
    }
}

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
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.camel.k.tooling.maven.support.IndexerSupport;
import org.apache.camel.k.tooling.maven.support.MavenSupport;
import org.apache.camel.util.AntPathMatcher;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.StringHelper;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;

@Mojo(
    name = "generate-yaml-schema",
    inheritByDefault = false,
    defaultPhase = LifecyclePhase.GENERATE_SOURCES,
    requiresDependencyResolution = ResolutionScope.COMPILE,
    threadSafe = true,
    requiresProject = false)
public class GenerateYamlSchema extends GenerateYamlSupport {
    @Parameter
    protected List<String> bannedDefinitions;
    @Parameter(property = "camel.k.yaml.schema", defaultValue = "${project.build.directory}/yaml-${project.version}.json")
    private File outputFile;

    private ObjectNode items;
    private ObjectNode definitions;

    @Override
    public void execute() throws MojoFailureException {
        final ObjectMapper mapper = new ObjectMapper();
        final ObjectNode root = mapper.createObjectNode();

        // Schema
        root.put("$schema", "http://json-schema.org/draft-04/schema#");
        root.put("type", "array");

        // Schema sections
        this.items = root.putObject("items");
        this.definitions = this.items.putObject("definitions");

        definitions(EXPRESSION_DEFINITION_CLASS).entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                processType(
                    definitions.with(entry.getValue().name().toString()),
                    entry.getValue());

                definitions.with("expressions")
                    .put("type", "object")
                    .with("properties")
                        .putObject(StringHelper.camelCaseToDash(entry.getKey()))
                        .put("$ref", "#/items/definitions/" + entry.getValue().name().toString());
            });
        definitions(DATAFORMAT_DEFINITION_CLASS).entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                processType(
                    definitions.with(entry.getValue().name().toString()),
                    entry.getValue());

                definitions.with("dataformats")
                    .put("type", "object")
                    .with("properties")
                        .putObject(StringHelper.camelCaseToDash(entry.getKey()))
                        .put("$ref", "#/items/definitions/" + entry.getValue().name().toString());
            });

        // custom node definitions
        annotated(YAML_NODE_DEFINITION_ANNOTATION)
            .sorted(Comparator.comparing(entry -> entry.name().toString()))
            .forEach(i -> processType(definitions.with(i.name().toString()), i));

        Set<String> ids = new HashSet<>();

        implementors(ERROR_HANDLER_CLASS)
            .sorted(Comparator.comparing(entry -> entry.name().toString()))
            .forEach(
                entry -> {
                    ObjectNode node = definitions.putObject(entry.name().toString());
                    if (hasStringConstructor(entry)) {
                        ArrayNode anyOf = node.putArray("anyOf");
                        anyOf.addObject()
                            .put("type", "string");

                        node = anyOf.addObject();
                    }

                    node.put("type", "object");

                    for (MethodInfo mi: IndexerSupport.methods(view.get(), entry)) {
                        if (mi.returnType().kind() != Type.Kind.VOID) {
                            continue;
                        }
                        if (mi.parameters().size() != 1) {
                            continue;
                        }
                        if (mi.parameters().get(0).kind() != Type.Kind.PRIMITIVE) {
                            continue;
                        }
                        if (!mi.name().startsWith("set")) {
                            continue;
                        }

                        String methodName = StringHelper.after(mi.name(), "set");
                        String propertyName = StringHelper.camelCaseToDash(methodName);
                        ObjectNode property = node.with("properties").with(propertyName);

                        setJsonSchemaType(property, mi.parameters().get(0));
                    }
                }
            );

        // custom parsers
        annotated(YAML_STEP_PARSER_ANNOTATION)
            .sorted(Comparator.comparing(entry -> entry.name().toString()))
            .forEach(
                entry -> {
                    boolean schema = annotationValue(entry, YAML_STEP_PARSER_ANNOTATION, "schema")
                        .map(AnnotationValue::asBoolean)
                        .orElse(true);

                    if (!schema) {
                        return;
                    }

                    String stepId = annotationValue(entry, YAML_STEP_PARSER_ANNOTATION, "id")
                        .map(AnnotationValue::asString)
                        .orElseThrow(() -> new IllegalArgumentException("Missing id field"));

                    if (!ids.add(stepId)) {
                        return;
                    }

                    String model = annotationValue(entry, YAML_STEP_PARSER_ANNOTATION, "definition")
                        .map(AnnotationValue::asString)
                        .orElseThrow(() -> new IllegalArgumentException("Missing definitions field"));

                    DotName name = DotName.createSimple(model);

                    if (implementsInterface(entry, START_STEP_PARSER_CLASS)) {
                        items.put("maxProperties", 1);
                        items.with("properties")
                            .putObject(stepId)
                            .put("$ref", "#/items/definitions/" + name.toString());
                    }

                    if (implementsInterface(entry, PROCESSOR_STEP_PARSER_CLASS)) {
                        ObjectNode stepNode = definitions.with("step");
                        stepNode.put("type", "object");
                        stepNode.put("maxProperties", 1);

                        stepNode.with("properties")
                            .putObject(stepId)
                            .put("$ref", "#/items/definitions/" + name.toString());
                    }
                }
            );

        // auto generated parsers
        annotated(XML_ROOT_ELEMENT_ANNOTATION_CLASS)
            .forEach(
                i -> {
                    AnnotationInstance meta = i.classAnnotation(METADATA_ANNOTATION);
                    AnnotationInstance xmlRoot = i.classAnnotation(XML_ROOT_ELEMENT_ANNOTATION_CLASS);

                    if (meta == null || xmlRoot == null) {
                        return;
                    }

                    AnnotationValue name = xmlRoot.value("name");
                    AnnotationValue label = meta.value("label");

                    if (name == null || label == null) {
                        return;
                    }

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
                        String stepId = StringHelper.camelCaseToDash(name.asString());
                        if (!ids.add(stepId)) {
                            return;
                        }

                        processType(definitions.with(i.name().toString()), i);

                        ObjectNode stepNode = definitions.with("step");
                        stepNode.put("type", "object");
                        stepNode.put("maxProperties", 1);

                        stepNode.with("properties")
                            .putObject(stepId)
                            .put("$ref", "#/items/definitions/" + i.name().toString());
                    }
                }
            );

        try {
            if (!outputFile.getParentFile().exists()) {
                outputFile.getParentFile().mkdirs();
            }

            mapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, root);
        } catch (IOException e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
    }

    protected void processType(ObjectNode root, ClassInfo type) {
        if (hasStringConstructor(type)) {
            ArrayNode anyOf = root.putArray("anyOf");
            anyOf.addObject()
                .put("type", "string");

            root = anyOf.addObject();
        }

        root.put("type", "object");

        boolean allOf = false;
        if (implementsInterface(type, HAS_EXPRESSION_CLASS)) {
            root.withArray("allOf").addObject().put("$ref", "#/items/definitions/expressions");
            allOf = true;
        }
        if (implementsInterface(type, HAS_DATAFORMAT_CLASS)) {
            root.withArray("allOf").addObject().put("$ref", "#/items/definitions/dataformats");
            allOf = true;
        }

        if (allOf) {
            root = root.withArray("allOf").addObject();
        }

        processFields(root, type);
        processMethods(root, type);
    }

    protected void processFields(ObjectNode root, ClassInfo type) {
        for (FieldInfo fi : IndexerSupport.fields(view.get(), type)) {
            if (fi.hasAnnotation(XML_TRANSIENT_CLASS)
                || fi.hasAnnotation(JSON_IGNORE_CLASS)) {
                continue;
            }
            if (!fi.hasAnnotation(XML_VALUE_ANNOTATION_CLASS)
                && !fi.hasAnnotation(XML_ATTRIBUTE_ANNOTATION_CLASS)
                && !fi.hasAnnotation(JSON_PROPERTY_CLASS)) {
                continue;
            }

            String fieldName = firstPresent(
                annotationValue(fi, XML_VALUE_ANNOTATION_CLASS, "name")
                    .map(AnnotationValue::asString)
                    .filter(value -> !"##default".equals(value)),
                annotationValue(fi, XML_ATTRIBUTE_ANNOTATION_CLASS, "name")
                    .map(AnnotationValue::asString)
                    .filter(value -> !"##default".equals(value)),
                annotationValue(fi, JSON_PROPERTY_CLASS, "value")
                    .map(AnnotationValue::asString)
                    .filter(value -> !"##default".equals(value))
            ).orElseGet(fi::name);

            String propertyName = StringHelper.camelCaseToDash(fieldName);
            ObjectNode property = root.with("properties").with(propertyName);

            setJsonSchemaType(property, fi.type());

            annotationValue(fi, METADATA_ANNOTATION, "defaultValue")
                .map(AnnotationValue::asString)
                .filter(ObjectHelper::isEmpty)
                .ifPresent(val -> property.put("default", val));
            annotationValue(fi, METADATA_ANNOTATION, "description")
                .map(AnnotationValue::asString)
                .filter(ObjectHelper::isEmpty)
                .ifPresent(val -> property.put("description", val));
            annotationValue(fi, METADATA_ANNOTATION, "enums")
                .map(AnnotationValue::asString)
                .ifPresent(val -> setEnum(property, "enum", val));

            if (isRequired(fi)) {
                root.withArray("required").add(propertyName);
            }
        }
    }

    protected void processMethods(ObjectNode root, ClassInfo type) {
        for (MethodInfo mi : IndexerSupport.methods(view.get(), type)) {
            if (mi.hasAnnotation(JSON_IGNORE_CLASS)) {
                continue;
            }
            if (!mi.hasAnnotation(JSON_PROPERTY_CLASS) && !mi.hasAnnotation(JSON_ALIAS_CLASS)) {
                continue;
            }
            if (mi.parameters().size() != 1) {
                continue;
            }

            String methodName = firstPresent(
                annotationValue(mi, JSON_ALIAS_CLASS, "value")
                    .map(AnnotationValue::asStringArray)
                    .filter(values -> values.length > 0)
                    .map(values -> values[0])
                    .filter(ObjectHelper::isNotEmpty),
                annotationValue(mi, JSON_PROPERTY_CLASS, "value")
                    .map(AnnotationValue::asString)
                    .filter(ObjectHelper::isNotEmpty)
            ).orElseGet(mi::name);

            if (methodName.startsWith("set")) {
                methodName = StringHelper.after(methodName, "set");
            }

            String propertyName = StringHelper.camelCaseToDash(methodName);
            ObjectNode property = root.with("properties").with(propertyName);

            Type param =  mi.parameters().get(0);

            // register types for classes
            if (param.kind() == Type.Kind.CLASS) {
                ClassInfo ci = view.get().getClassByName(param.name());
                if (ci != null && !definitions.has(ci.name().toString())) {
                    processType(definitions.putObject(ci.name().toString()), ci);
                }
            }

            setJsonSchemaType(property, param);

            annotationValue(mi, METADATA_ANNOTATION, "defaultValue")
                .map(AnnotationValue::asString)
                .filter(ObjectHelper::isEmpty)
                .ifPresent(val -> property.put("default", val));
            annotationValue(mi, METADATA_ANNOTATION, "description")
                .map(AnnotationValue::asString)
                .filter(ObjectHelper::isEmpty)
                .ifPresent(val -> property.put("description", val));
            annotationValue(mi, METADATA_ANNOTATION, "enums")
                .map(AnnotationValue::asString)
                .ifPresent(val -> setEnum(property, "enum", val));

            if (isRequired(mi)) {
                root.withArray("required").add(propertyName);
            }
        }
    }

    // *******************************************
    //
    // Helpers
    //
    // *******************************************

    protected boolean isRequired(FieldInfo fi) {
        return firstPresent(
            annotationValue(fi, METADATA_ANNOTATION, "required")
                .map(AnnotationValue::asBoolean),
            annotationValue(fi, JSON_PROPERTY_CLASS, "required")
                .map(AnnotationValue::asBoolean),
            annotationValue(fi, XML_VALUE_ANNOTATION_CLASS, "required")
                .map(AnnotationValue::asBoolean)
        ).orElse(false);
    }

    protected boolean isRequired(MethodInfo mi) {
        return firstPresent(
            annotationValue(mi, METADATA_ANNOTATION, "required")
                .map(AnnotationValue::asBoolean),
            annotationValue(mi, JSON_PROPERTY_CLASS, "required")
                .map(AnnotationValue::asBoolean)
        ).orElse(false);
    }

    protected static void setEnum(ObjectNode root, String name, String enumValues) {
        ObjectHelper.notNull(root, "root");
        ObjectHelper.notNull(name, "name");

        if (ObjectHelper.isEmpty(enumValues)) {
            return;
        }

        ArrayNode array = root.putArray(name);
        for (String enumValue: enumValues.split(",")) {
            array.add(enumValue);
        }
    }

    protected static boolean hasStringConstructor(ClassInfo type) {
        DotName javaLangString = DotName.createSimple("java.lang.String");

        for (MethodInfo mi: type.methods()) {
            if (!"<init>".equals(mi.name())) {
                continue;
            }
            if (mi.parameters().size() != 1) {
                continue;
            }
            if (mi.parameters().get(0).name().equals(javaLangString)) {
                return true;
            }
        }

        return false;
    }

    protected static boolean implementsInterface(ClassInfo type, DotName interfaceName) {
        return type.interfaceNames().stream().anyMatch(i -> i.equals(interfaceName));
    }

    protected void setJsonSchemaType(ObjectNode node, Type type) {
        if (type.kind() == Type.Kind.PARAMETERIZED_TYPE) {
            ParameterizedType parameterized = type.asParameterizedType();

            if (parameterized.arguments().size() == 1) {
                final Type parametrizedType = parameterized.arguments().get(0);
                final ClassInfo parametrizedTypeInfo = view.get().getClassByName(parametrizedType.name());

                if (parameterized.name().equals(LIST_CLASS) && parametrizedTypeInfo != null) {
                    if (parametrizedTypeInfo.name().equals(STEP_CLASS)) {
                        node.put("type", "array");
                        node.with("items").put("$ref", "#/items/definitions/step");
                        return;
                    }
                    if (parametrizedTypeInfo.classAnnotation(YAML_NODE_DEFINITION_ANNOTATION) != null) {
                        node.put("type", "array");
                        node.with("items").put("$ref", "#/items/definitions/" + parametrizedTypeInfo.name().toString());
                        return;
                    }
                }
            }
        }

        final ClassInfo typeClass = view.get().getClassByName(type.name());
        if (typeClass != null && typeClass.classAnnotation(YAML_NODE_DEFINITION_ANNOTATION) != null) {
            node.put("$ref", "#/items/definitions/" + type.name().toString());
            return;
        }

        final String javaType = type.name().toString();
        switch (javaType) {
            /*
             * <tr><th scope="row"> boolean            <td style="text-align:center"> Z
             * <tr><th scope="row"> byte               <td style="text-align:center"> B
             * <tr><th scope="row"> char               <td style="text-align:center"> C
             * <tr><th scope="row"> class or interface <td style="text-align:center"> L<i>classname</i>;
             * <tr><th scope="row"> double             <td style="text-align:center"> D
             * <tr><th scope="row"> float              <td style="text-align:center"> F
             * <tr><th scope="row"> int                <td style="text-align:center"> I
             * <tr><th scope="row"> long               <td style="text-align:center"> J
             * <tr><th scope="row"> short              <td style="text-align:center"> S
             */
            case "java.lang.Class":
                node.put("type", "string");
                break;
            case "[B":
                node.put("type", "string");
                node.put("format", "binary");
                break;
            case "[Ljava.lang.Class;":
                node.put("type", "array");
                node.with("items").put("type", "string");
                break;
            case "boolean":
                node.put("type", "boolean");
                break;
            case "char":
                node.put("type", "string");
                break;
            case "int":
            case "float":
            case "long":
            case "double":
                node.put("type", "number");
                break;
            default:
                if (definitions.has(javaType)) {
                    node.put("$ref", "#/items/definitions/" + javaType);
                } else {
                    try {
                        Class<?> clazz = MavenSupport.getClassLoader(project).loadClass(javaType);

                        if (clazz.isEnum()) {
                            ArrayNode array = node.putArray("enum");
                            for (Object t : clazz.getEnumConstants()) {
                                array.add(((Enum) t).name());
                            }
                        } else if (CharSequence.class.isAssignableFrom(clazz)) {
                            node.put("type", "string");
                        } else if (Boolean.class.isAssignableFrom(clazz)) {
                            node.put("type", "boolean");
                        } else if (Number.class.isAssignableFrom(clazz)) {
                            node.put("type", "number");
                        } else if (Collection.class.isAssignableFrom(clazz)) {
                            node.put("type", "array");
                            node.with("items").put("type", "string");
                        } else if (Map.class.isAssignableFrom(clazz)) {
                            node.put("type", "object");
                        } else {
                            throw new IllegalStateException("Unknown java_type: " + javaType + " on node: " + node);
                        }
                    } catch (ClassNotFoundException e) {
                        throw new IllegalStateException("Unknown java_type: " + javaType + " on node: " + node, e);
                    }
                }
        }
    }


    @SafeVarargs
    protected final <T> Optional<T> firstPresent(Optional<T>... optionals) {
        for (Optional<T> optional: optionals) {
            if (optional.isPresent()) {
                return optional;
            }
        }

        return Optional.empty();

    }
}

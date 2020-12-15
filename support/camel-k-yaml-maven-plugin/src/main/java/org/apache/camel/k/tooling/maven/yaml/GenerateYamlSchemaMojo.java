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


import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.camel.k.tooling.maven.yaml.suport.IndexerSupport;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.StringHelper;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
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
public class GenerateYamlSchemaMojo extends GenerateYamlSupportMojo {
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

        // Try to set-up definitions first
        for (ClassInfo ci: definitions()) {
            if (!definitions.has(ci.name().toString())) {
                processType(definitions.with(ci.name().toString()), ci);
            }
        }

        elementsOf(EXPRESSION_DEFINITION_CLASS).entrySet().stream()
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
        elementsOf(DATAFORMAT_DEFINITION_CLASS).entrySet().stream()
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

        for (var entry: models().entrySet()) {
            final String typeName = entry.getValue().name().toString();
            final String stepId = StringHelper.camelCaseToDash(entry.getKey());

            if (ids.add(stepId)) {
                ObjectNode stepNode = definitions.with("step");
                stepNode.put("type", "object");
                stepNode.put("maxProperties", 1);

                stepNode.with("properties")
                    .putObject(stepId)
                    .put("$ref", "#/items/definitions/" + typeName);
            }
        }

        if (!outputFile.getParentFile().exists() && !outputFile.getParentFile().mkdirs()) {
            throw new MojoFailureException("Unable to create directory " + outputFile.getParentFile());
        }

        try {
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
        if (implementsInterface(type, HAS_EXPRESSION_CLASS) ||
            extendsClass(type, EXPRESSION_NODE_CLASS)) {
            root.withArray("allOf").addObject().put("$ref", "#/items/definitions/expressions");
            allOf = true;
        }
        if (implementsInterface(type, HAS_DATAFORMAT_CLASS) ||
            extendsClass(type, MARSHAL_DEFINITION_CLASS) ||
            extendsClass(type, UNMARSHAL_DEFINITION_CLASS)) {
            root.withArray("allOf").addObject().put("$ref", "#/items/definitions/dataformats");
            allOf = true;
        }

        if (allOf) {
            root = root.withArray("allOf").addObject();
        }
        if (implementsInterface(type, OUTPUT_NODE_CLASS)) {
            root.with("properties")
                .with("steps")
                .put("type", "array")
                .with("items")
                    .put("$ref", "#/items/definitions/step");
        }

        processFields(root, type);
        //processMethods(root, type);
    }

    protected void processFields(ObjectNode root, ClassInfo type) {
        for (FieldInfo fi : IndexerSupport.fields(view.get(), type)) {
            if (fi.hasAnnotation(XML_TRANSIENT_CLASS) ||
                fi.hasAnnotation(JSON_IGNORE_CLASS)) {
                continue;
            }
            if (!fi.hasAnnotation(XML_VALUE_ANNOTATION_CLASS) &&
                !fi.hasAnnotation(XML_ATTRIBUTE_ANNOTATION_CLASS) &&
                !fi.hasAnnotation(XML_ELEMENT_ANNOTATION_CLASS) &&
                !fi.hasAnnotation(JSON_PROPERTY_CLASS)) {
                continue;
            }

            String fieldName = firstPresent(
                annotationValue(fi, XML_VALUE_ANNOTATION_CLASS, "name")
                    .map(AnnotationValue::asString)
                    .filter(value -> !"##default".equals(value)),
                annotationValue(fi, XML_ATTRIBUTE_ANNOTATION_CLASS, "name")
                    .map(AnnotationValue::asString)
                    .filter(value -> !"##default".equals(value)),
                annotationValue(fi, XML_ELEMENT_ANNOTATION_CLASS, "name")
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
                ArrayNode required = root.withArray("required");
                boolean add = true;

                Iterator<JsonNode> elements = required.elements();
                while (elements.hasNext()) {
                    JsonNode element = elements.next();
                    if (element.asText().equals(propertyName)) {
                        add = false;
                        break;
                    }
                }

                if (add) {
                    required.add(propertyName);
                }
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

    protected boolean implementsInterface(ClassInfo type, DotName interfaceName) {
        if (type.interfaceNames().stream().anyMatch(i -> i.equals(interfaceName))) {
            return true;
        }

        if (type.superName() != null) {
            ClassInfo s = view.get().getClassByName(type.superName());
            if (s == null) {
                return false;
            }

            return implementsInterface(s, interfaceName);
        }
        return false;
    }

    protected boolean extendsClass(ClassInfo type, DotName className) {
        if (type == null) {
            return false;
        }

        if (type.name().equals(className) || type.superName().equals(className)) {
            return true;
        }
        if (type.superName() != null && type.superName().equals(className)) {
            return true;
        }
        if (type.superName() != null) {
            ClassInfo s = view.get().getClassByName(type.superName());
            if (s == null) {
                return false;
            }

            return extendsClass(s, className);
        }

        return false;
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
                        Class<?> clazz = IndexerSupport.getClassLoader(project).loadClass(javaType);
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

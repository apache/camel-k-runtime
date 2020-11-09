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


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.camel.k.tooling.maven.support.IndexerSupport;
import org.apache.camel.k.tooling.maven.support.MavenSupport;
import org.apache.camel.util.function.Suppliers;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

public abstract class GenerateYamlSupport extends AbstractMojo {

    public static final DotName LIST_CLASS =
        DotName.createSimple("java.util.List");

    public static final DotName XML_ROOT_ELEMENT_ANNOTATION_CLASS =
        DotName.createSimple("javax.xml.bind.annotation.XmlRootElement");
    public static final DotName XML_ATTRIBUTE_ANNOTATION_CLASS =
        DotName.createSimple("javax.xml.bind.annotation.XmlAttribute");
    public static final DotName XML_VALUE_ANNOTATION_CLASS =
        DotName.createSimple("javax.xml.bind.annotation.XmlValue");
    public static final DotName XML_TRANSIENT_CLASS =
        DotName.createSimple("javax.xml.bind.annotation.XmlTransient");

    public static final DotName JSON_PROPERTY_CLASS =
        DotName.createSimple("com.fasterxml.jackson.annotation.JsonProperty");
    public static final DotName JSON_IGNORE_CLASS =
        DotName.createSimple("com.fasterxml.jackson.annotation.JsonIgnore");
    public static final DotName JSON_ALIAS_CLASS =
        DotName.createSimple("com.fasterxml.jackson.annotation.JsonAlias");

    public static final DotName METADATA_ANNOTATION =
        DotName.createSimple("org.apache.camel.spi.Metadata");
    public static final DotName EXPRESSION_DEFINITION_CLASS =
        DotName.createSimple("org.apache.camel.model.language.ExpressionDefinition");
    public static final DotName DATAFORMAT_DEFINITION_CLASS =
        DotName.createSimple("org.apache.camel.model.DataFormatDefinition");
    public static final DotName ERROR_HANDLER_CLASS =
        DotName.createSimple("org.apache.camel.builder.ErrorHandlerBuilder");

    public static final DotName YAML_NODE_DEFINITION_ANNOTATION =
        DotName.createSimple("org.apache.camel.k.annotation.yaml.YAMLNodeDefinition");
    public static final DotName YAML_STEP_PARSER_ANNOTATION =
        DotName.createSimple("org.apache.camel.k.annotation.yaml.YAMLStepParser");
    public static final DotName YAML_MIXIN_ANNOTATION =
        DotName.createSimple("org.apache.camel.k.annotation.yaml.YAMLMixIn");
    public static final DotName JSON_SCHEMA_IGNORE_ANNOTATION =
        DotName.createSimple("org.apache.camel.k.annotation.yaml.JsonSchemaIgnore");
    public static final DotName LOAD_BALANCE_DEFINITION_CLASS =
        DotName.createSimple("org.apache.camel.model.LoadBalancerDefinition");
    public static final DotName START_STEP_PARSER_CLASS =
        DotName.createSimple("org.apache.camel.k.loader.yaml.spi.StartStepParser");
    public static final DotName PROCESSOR_STEP_PARSER_CLASS =
        DotName.createSimple("org.apache.camel.k.loader.yaml.spi.ProcessorStepParser");
    public static final DotName HAS_EXPRESSION_CLASS =
        DotName.createSimple("org.apache.camel.k.loader.yaml.parser.HasExpression");
    public static final DotName HAS_DATAFORMAT_CLASS =
        DotName.createSimple("org.apache.camel.k.loader.yaml.parser.HasDataFormat");
    public static final DotName HAS_ENDPOINT_CONSUMER_CLASS =
        DotName.createSimple("org.apache.camel.k.loader.yaml.parser.HasEndpointConsumer");
    public static final DotName HAS_ENDPOINT_PRODUCER_CLASS =
        DotName.createSimple("org.apache.camel.k.loader.yaml.parser.HasEndpointProducer");
    public static final DotName HAS_URI_PRODUCER_CLASS =
        DotName.createSimple("org.apache.camel.k.loader.yaml.parser.HasUri");
    public static final DotName STEP_CLASS =
        DotName.createSimple("org.apache.camel.k.loader.yaml.model.Step");


    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    protected final Supplier<IndexView> view;

    GenerateYamlSupport() {
        this.view = Suppliers.memorize(() -> IndexerSupport.get(project));
    }

    protected Map<String, ClassInfo> definitions(DotName type) {
        Map<String, ClassInfo> definitions = new HashMap<>();

        for (ClassInfo ci: view.get().getAllKnownSubclasses(type)) {
            AnnotationInstance instance = ci.classAnnotation(XML_ROOT_ELEMENT_ANNOTATION_CLASS);
            if (instance != null) {
                AnnotationValue name = instance.value("name");
                if (name != null) {
                    definitions.put(name.asString(), ci);
                }
            }
        }

        return Collections.unmodifiableMap(definitions);
    }

    protected Stream<ClassInfo> implementors(DotName type) {
        return view.get().getAllKnownImplementors(type).stream();
    }

    protected Stream<ClassInfo> annotated(DotName type) {
        return view.get().getAnnotations(type).stream()
            .map(AnnotationInstance::target)
            .filter(t -> t.kind() == AnnotationTarget.Kind.CLASS)
            .map(AnnotationTarget::asClass);
    }

    protected Optional<AnnotationValue> annotationValue(AnnotationInstance instance, String name) {
        return instance != null
            ? Optional.ofNullable(instance.value(name))
            : Optional.empty();
    }

    protected Optional<AnnotationValue> annotationValue(ClassInfo target, DotName annotationName, String name) {
        return annotationValue(
            target.classAnnotation(annotationName),
            name
        );
    }

    protected Optional<AnnotationValue> annotationValue(FieldInfo target, DotName annotationName, String name) {
        return annotationValue(
            target.annotation(annotationName),
            name
        );
    }


    protected Optional<AnnotationValue> annotationValue(MethodInfo target, DotName annotationName, String name) {
        return annotationValue(
            target.annotation(annotationName),
            name
        );
    }


    protected Class<?> loadClass(ClassInfo ci) {
        return loadClass(ci.name().toString());
    }

    protected Class<?> loadClass(String className) {
        try {
            return MavenSupport.getClassLoader(project).loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}

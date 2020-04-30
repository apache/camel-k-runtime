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
package org.apache.camel.k.loader.yaml.quarkus.deployment;

import java.util.stream.Collectors;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.apache.camel.k.loader.yaml.model.Node;
import org.apache.camel.k.loader.yaml.model.Step;
import org.apache.camel.k.loader.yaml.parser.HasDataFormat;
import org.apache.camel.k.loader.yaml.parser.HasExpression;
import org.apache.camel.k.loader.yaml.parser.StepParser;
import org.apache.camel.k.loader.yaml.support.Any23DataFormatMixIn;
import org.apache.camel.k.loader.yaml.support.ExpressionNodeMixIn;
import org.apache.camel.k.loader.yaml.support.ProcessorDefinitionMixIn;
import org.apache.camel.k.loader.yaml.support.XStreamDataFormatMixIn;
import org.apache.camel.model.DataFormatDefinition;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.language.ExpressionDefinition;
import org.apache.camel.quarkus.core.deployment.CamelServicePatternBuildItem;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

public class DeploymentProcessor {
    public static final DotName YAML_STEP_PARSER_ANNOTATION = DotName.createSimple("org.apache.camel.k.annotation.yaml.YAMLStepParser");
    public static final DotName YAML_STEP_DEFINITION_ANNOTATION = DotName.createSimple("org.apache.camel.k.annotation.yaml.YAMLNodeDefinition");

    @BuildStep
    CamelServicePatternBuildItem servicePatterns() {
        return new CamelServicePatternBuildItem(
            CamelServicePatternBuildItem.CamelServiceDestination.REGISTRY,
            true,
            StepParser.SERVICE_LOCATION + "/*");
    }

    @BuildStep
    void registerReflectiveClasses(
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            CombinedIndexBuildItem combinedIndexBuildItem) {

        IndexView view = combinedIndexBuildItem.getIndex();

        for (ClassInfo ci : getAnnotated(view, YAML_STEP_PARSER_ANNOTATION)) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, ci.name().toString()));
        }
        for (ClassInfo ci : getAnnotated(view, YAML_STEP_DEFINITION_ANNOTATION)) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, ci.name().toString()));
        }

        for (ClassInfo ci : getAllKnownImplementors(view, Step.Deserializer.class)) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, ci.name().toString()));
        }
        for (ClassInfo ci : getAllKnownSubclasses(view, ProcessorDefinition.class)) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, ci.name().toString()));
        }
        for (ClassInfo ci : getAllKnownSubclasses(view, ExpressionDefinition.class)) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, ci.name().toString()));
        }
        for (ClassInfo ci : getAllKnownSubclasses(view, DataFormatDefinition.class)) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, ci.name().toString()));
        }

        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, Step.Deserializer.class));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, HasExpression.class));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, HasDataFormat.class));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, Node.class));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, ProcessorDefinitionMixIn.class));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, ExpressionNodeMixIn.class));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, Any23DataFormatMixIn.class));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, XStreamDataFormatMixIn.class));
    }

    private static Iterable<ClassInfo> getAllKnownImplementors(IndexView view, String name) {
        return getAllKnownImplementors(view, DotName.createSimple(name));
    }

    private static Iterable<ClassInfo> getAllKnownImplementors(IndexView view, Class<?> type) {
        return getAllKnownImplementors(view, DotName.createSimple(type.getName()));
    }

    private static Iterable<ClassInfo> getAllKnownImplementors(IndexView view, DotName type) {
        return view.getAllKnownImplementors(type);
    }

    private static Iterable<ClassInfo> getAllKnownSubclasses(IndexView view, String name) {
        return getAllKnownSubclasses(view, DotName.createSimple(name));
    }

    private static Iterable<ClassInfo> getAllKnownSubclasses(IndexView view, Class<?> type) {
        return getAllKnownSubclasses(view, DotName.createSimple(type.getName()));
    }

    private static Iterable<ClassInfo> getAllKnownSubclasses(IndexView view, DotName type) {
        return view.getAllKnownSubclasses(type);
    }

    private static Iterable<ClassInfo> getAnnotated(IndexView view, String name) {
        return getAnnotated(view, DotName.createSimple(name));
    }

    private static Iterable<ClassInfo> getAnnotated(IndexView view, Class<?> type) {
        return getAnnotated(view, DotName.createSimple(type.getName()));
    }

    private static Iterable<ClassInfo> getAnnotated(IndexView view, DotName type) {
        return view.getAnnotations(type).stream()
            .map(AnnotationInstance::target)
            .filter(t -> t.kind() == AnnotationTarget.Kind.CLASS)
            .map(AnnotationTarget::asClass)
            .collect(Collectors.toList());
    }
}

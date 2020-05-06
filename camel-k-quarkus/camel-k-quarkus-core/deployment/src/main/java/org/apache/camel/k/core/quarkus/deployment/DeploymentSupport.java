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
package org.apache.camel.k.core.quarkus.deployment;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

public final class DeploymentSupport {
    private DeploymentSupport() {
    }

    public static Iterable<ClassInfo> getAllKnownImplementors(IndexView view, String name) {
        return view.getAllKnownImplementors(DotName.createSimple(name));
    }

    public static Iterable<ClassInfo> getAllKnownImplementors(IndexView view, Class<?> type) {
        return view.getAllKnownImplementors(DotName.createSimple(type.getName()));
    }

    public static Iterable<ClassInfo> getAllKnownSubclasses(IndexView view, String name) {
        return view.getAllKnownSubclasses(DotName.createSimple(name));
    }

    public static Iterable<ClassInfo> getAllKnownSubclasses(IndexView view, Class<?> type) {
        return view.getAllKnownSubclasses(DotName.createSimple(type.getName()));
    }

    public static Iterable<ClassInfo> getAllKnownImplementors(IndexView view, DotName type) {
        return view.getAllKnownImplementors(type);
    }

    public static Iterable<ClassInfo> getAllKnownSubclasses(IndexView view, DotName type) {
        return view.getAllKnownSubclasses(type);
    }

    public static Iterable<ClassInfo> getAnnotated(IndexView view, String name) {
        return getAnnotated(view, DotName.createSimple(name));
    }

    public static Iterable<ClassInfo> getAnnotated(IndexView view, Class<?> type) {
        return getAnnotated(view, DotName.createSimple(type.getName()));
    }

    public static Iterable<ClassInfo> getAnnotated(IndexView view, DotName type) {
        return view.getAnnotations(type).stream()
            .map(AnnotationInstance::target)
            .filter(t -> t.kind() == AnnotationTarget.Kind.CLASS)
            .map(AnnotationTarget::asClass)
            .collect(Collectors.toList());
    }


    public static ReflectiveClassBuildItem reflectiveClassBuildItem(ClassInfo... classInfos) {
        return new ReflectiveClassBuildItem(
            true,
            false,
            Stream.of(classInfos)
                .map(ClassInfo::name)
                .map(DotName::toString)
                .toArray(String[]::new)
        );
    }

    public static ReflectiveClassBuildItem reflectiveClassBuildItem(boolean methods, boolean fields, ClassInfo... classInfos) {
        return new ReflectiveClassBuildItem(
            methods,
            fields,
            Stream.of(classInfos)
                .map(ClassInfo::name)
                .map(DotName::toString)
                .toArray(String[]::new)
        );
    }

}

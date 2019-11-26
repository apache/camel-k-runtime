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
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexReader;
import org.jboss.jandex.IndexView;

public abstract class GenerateYamlSupport extends AbstractMojo {
    public static final DotName EXPRESSION_DEFINITION_CLASS = DotName.createSimple("org.apache.camel.model.language.ExpressionDefinition");
    public static final DotName DATAFORMAT_DEFINITION_CLASS = DotName.createSimple("org.apache.camel.model.DataFormatDefinition");
    public static final DotName XMLROOTELEMENT_ANNOTATION_CLASS = DotName.createSimple("javax.xml.bind.annotation.XmlRootElement");
    public static final DotName YAML_STEP_DEFINITION_CLASS = DotName.createSimple("org.apache.camel.k.loader.yaml.model.Step$Definition");
    public static final DotName YAML_STEP_PARSER_ANNOTATION = DotName.createSimple("org.apache.camel.k.annotation.yaml.YAMLStepParser");
    public static final DotName YAML_STEP_DEFINITION_ANNOTATION = DotName.createSimple("org.apache.camel.k.annotation.yaml.YAMLNodeDefinition");

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;
    @Parameter(defaultValue = "${project.build.directory}/generated-sources/camel")
    protected String output;

    protected Map<String, Class<?>> definitions(DotName type) {
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

    protected Stream<ClassInfo> implementors(DotName type) {
        ClassLoader cl = getClassLoader();
        IndexView view = getCompositeIndexer(cl);

        return view.getAllKnownImplementors(type).stream();
    }

    protected Stream<ClassInfo> annotated(DotName type) {
        ClassLoader cl = getClassLoader();
        IndexView view = getCompositeIndexer(cl);


        return view.getAnnotations(type).stream()
            .map(AnnotationInstance::target)
            .filter(t -> t.kind() == AnnotationTarget.Kind.CLASS)
            .map(AnnotationTarget::asClass);
    }

    protected static IndexView getCompositeIndexer(ClassLoader classLoader) {
        try {
            Enumeration<URL> elements = classLoader.getResources("META-INF/jandex.idx");
            List<IndexView> allIndex = new ArrayList<>();
            Set<URL> locations = new HashSet<>();

            for (Enumeration<URL> e = elements; e.hasMoreElements();) {
                URL url = e.nextElement();
                if (locations.add(url)) {
                    try (InputStream is = url.openStream()) {
                        allIndex.add(new IndexReader(is).read());
                    }
                }
            }

            return CompositeIndex.create(allIndex);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected ClassLoader getClassLoader() {
        if (project == null) {
            return getClass().getClassLoader();
        }

        try {
            List<String> elements = new ArrayList<>();
            elements.addAll(project.getCompileClasspathElements());

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

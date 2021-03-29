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
package org.apache.camel.k.tooling.apt;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.function.BiConsumer;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import org.apache.camel.k.annotation.Customizer;
import org.apache.camel.k.annotation.LoaderInterceptor;

@SupportedAnnotationTypes({
    "org.apache.camel.k.annotation.Customizer",
    "org.apache.camel.k.annotation.LoaderInterceptor",
})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class CamelProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            Set<? extends Element> ae = roundEnv.getElementsAnnotatedWith(annotation);
            for (Element element: ae) {
                on(element, LoaderInterceptor.class, (e, a) -> {
                    service(
                        output("META-INF/services/org/apache/camel/k/loader/interceptor/%s", a.value()),
                        e
                    );
                });
                on(element, Customizer.class, (e, a) -> {
                    service(
                        output("META-INF/services/org/apache/camel/k/customizer/%s", a.value()),
                        e
                    );
                });
            }
        }

        return false;
    }

    // ******************************
    //
    // helpers
    //
    // ******************************

    private Path output(String fmt, Object... args) {
        try {
            FileObject result;

            try {
                result = processingEnv.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", String.format(fmt, args));
            } catch (IOException e) {
                result = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", String.format(fmt, args));
            }

            Path answer = Paths.get(result.toUri());

            if (!Files.exists(answer.getParent())) {
                Files.createDirectories(answer.getParent());
            }

            return answer;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void service(Path target, String type) {
        try {
            Files.write(
                target,
                String.format("class=%s", type).getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void service(Path target, TypeElement type) {
        service(target, type.getQualifiedName().toString());
    }

    private <T extends Annotation> void on(Element element, Class<T> annotationType, BiConsumer<TypeElement, T> consumer) {
        if (element instanceof TypeElement) {
            T annotation = element.getAnnotation(annotationType);
            if (annotation != null) {
                consumer.accept((TypeElement) element, annotation);
            }
        }
    }
}

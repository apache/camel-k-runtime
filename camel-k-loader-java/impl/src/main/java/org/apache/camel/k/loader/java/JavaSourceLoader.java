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
package org.apache.camel.k.loader.java;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.k.CompositeClassloader;
import org.apache.camel.k.Source;
import org.apache.camel.k.SourceLoader;
import org.apache.camel.k.annotation.Loader;
import org.apache.camel.k.support.StringSupport;
import org.apache.camel.util.IOHelper;
import org.joor.Reflect;

@Loader(value = "java")
public class JavaSourceLoader implements SourceLoader {
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("^\\s*package\\s+([a-zA-Z][\\.\\w]*)\\s*;.*$", Pattern.MULTILINE);

    @Override
    public Collection<String> getSupportedLanguages() {
        return Collections.singletonList("java");
    }

    @Override
    public RoutesBuilder load(CamelContext camelContext, Source source) {
        try (InputStream is = source.resolveAsInputStream(camelContext)) {
            final String content = IOHelper.loadText(is);
            final String name = determineQualifiedName(source, content);
            final Reflect compiled = Reflect.compile(name, content);
            final RoutesBuilder instance = compiled.create().get();

            // The given source may contains additional nested classes which are unknown to Camel
            // as they are associated to the ClassLoader used to compile the source thus we need
            // to add it to the ApplicationContextClassLoader.
            final ClassLoader loader = camelContext.getApplicationContextClassLoader();
            if (loader instanceof CompositeClassloader) {
                ((CompositeClassloader) loader).addClassLoader(instance.getClass().getClassLoader());
            }

            return instance;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String determineQualifiedName(Source source, String content) {
        final String name = StringSupport.substringBefore(source.getName(), ".java");
        final Matcher matcher = PACKAGE_PATTERN.matcher(content);

        return matcher.find()
            ? matcher.group(1) + "." + name
            : name;
    }
}

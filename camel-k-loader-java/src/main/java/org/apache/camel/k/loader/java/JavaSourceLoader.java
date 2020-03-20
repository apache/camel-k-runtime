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

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.camel.k.Runtime;
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
    public List<String> getSupportedLanguages() {
        return Collections.singletonList("java");
    }

    @Override
    public Result load(Runtime runtime, Source source) throws Exception {
        try (InputStream is = source.resolveAsInputStream(runtime.getCamelContext())) {
            final String content = IOHelper.loadText(is);
            final String name = determineQualifiedName(source, content);
            final Reflect compiled = Reflect.compile(name, content);
            final Object instance = compiled.create().get();

            return Result.on(instance);
        }
    }

    private static String determineQualifiedName(Source source, String content) {
        String name = StringSupport.substringBefore(source.getName(), ".java");
        Matcher matcher = PACKAGE_PATTERN.matcher(content);

        if (matcher.find()) {
            name = matcher.group(1) + "." + name;
        }

        return name;
    }
}

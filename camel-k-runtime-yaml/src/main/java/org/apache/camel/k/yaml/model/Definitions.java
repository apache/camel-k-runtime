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
package org.apache.camel.k.yaml.model;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.apache.camel.model.DataFormatDefinition;
import org.apache.camel.model.language.ExpressionDefinition;
import org.apache.commons.io.IOUtils;

public final class Definitions {
    public static final Map<String, Class<? extends DataFormatDefinition>> DATA_FORMATS_MAP = dataFormatDefinition();
    public static final Map<String, Class<? extends ExpressionDefinition>> EXPRESSIONS_MAP = expressionDefinition();

    private Definitions() {
    }

    public static Map<String, Class<? extends DataFormatDefinition>> dataFormatDefinition() {
        Map<String, Class<? extends DataFormatDefinition>> definitions = loadDefinitionFromJaxbIndex("org.apache.camel.model.dataformat");

        return Collections.unmodifiableMap(definitions);
    }

    public static Map<String, Class<? extends ExpressionDefinition>> expressionDefinition() {
        Map<String, Class<? extends ExpressionDefinition>> definitions = loadDefinitionFromJaxbIndex("org.apache.camel.model.language");

        // TODO: need to override tokenizer till camel M4 is out as the one from camel does
        //       not support constructing it from string in M3
        definitions.put("tokenizer", TokenizerExpression.class);

        return Collections.unmodifiableMap(definitions);
    }

    @SuppressWarnings("unchecked")
    public static <T> Map<String, Class<? extends T>> loadDefinitionFromJaxbIndex(String packageName) {
        Map<String, Class<? extends T>> definitions = new HashMap<>();
        String path = "/" + packageName.replace(".", "/") + "/jaxb.index";

        try (InputStream is = Definitions.class.getResourceAsStream(path)) {
            for (String name : IOUtils.readLines(is, StandardCharsets.UTF_8)) {
                name = name.trim();

                if (!name.startsWith("#") && !name.isEmpty()) {
                    ClassLoader cl = Thread.currentThread().getContextClassLoader();
                    Class<?> clazz = cl.loadClass(packageName + "." + name);
                    XmlRootElement root = clazz.getAnnotation(XmlRootElement.class);

                    if (root != null) {
                        definitions.put(root.name(), (Class<T>) clazz);
                    }
                }
            }
        } catch (IOException | NoClassDefFoundError | ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }

        return definitions;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class DataFormat {
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
        public org.apache.camel.model.DataFormatDefinition dataFormat;
    }

    public static class TokenizerExpression extends org.apache.camel.model.language.TokenizerExpression {

        public TokenizerExpression() {
        }
        public TokenizerExpression(String token) {
            super.setToken(token);
        }
    }

}

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
package org.apache.camel.k;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

import org.apache.camel.CamelContext;
import org.apache.camel.k.support.StringSupport;
import org.apache.camel.spi.ClassResolver;
import org.apache.camel.support.ResourceHelper;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.URISupport;

public final class  Sources {
    private Sources() {
    }

    public static Source fromURI(String uri) throws Exception {
        return new URI(uri);
    }

    public static Source fromBytes(String name, String language, String loader, List<String> interceptors, byte[] content) {
        return new InMemory(name, language, loader, interceptors, content);
    }

    public static Source fromBytes(String name, String language, String loader, byte[] content) {
        return new InMemory(name, language, loader, content);
    }

    public static Source fromBytes(String language, byte[] content) {
        return new InMemory(UUID.randomUUID().toString(), language, null, content);
    }

    private static final class InMemory implements Source {
        private final String name;
        private final String language;
        private final String loader;
        private final List<String> interceptors;
        private final byte[] content;

        public InMemory(String name, String language, String loader, byte[] content) {
            this.name = name;
            this.language = language;
            this.loader = loader;
            this.interceptors = Collections.emptyList();
            this.content = Arrays.copyOf(content, content.length);
        }

        public InMemory(String name, String language, String loader, List<String> interceptors, byte[] content) {
            this.name = name;
            this.language = language;
            this.loader = loader;
            this.interceptors = new ArrayList<>(interceptors);
            this.content = Arrays.copyOf(content, content.length);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getLanguage() {
            return language;
        }

        @Override
        public Optional<String> getLoader() {
            return Optional.ofNullable(loader);
        }

        @Override
        public List<String> getInterceptors() {
            return interceptors;
        }

        @Override
        public InputStream resolveAsInputStream(CamelContext ctx) {
            if (content == null) {
                throw new IllegalArgumentException("No content defined");
            }

            return new ByteArrayInputStream(this.content);
        }
    }

    private static final class URI implements Source {
        private final String location;
        private final String name;
        private final String language;
        private final String loader;
        private final String interceptors;
        private final boolean compressed;

        private URI(String uri) throws Exception {
            final String location = StringSupport.substringBefore(uri, "?");

            if (!location.startsWith(Constants.SCHEME_PREFIX_CLASSPATH) && !location.startsWith(Constants.SCHEME_PREFIX_FILE)) {
                throw new IllegalArgumentException("No valid resource format, expected scheme:path, found " + uri);
            }

            final String query = StringSupport.substringAfter(uri, "?");
            final Map<String, Object> params = URISupport.parseQuery(query);

            String language = (String) params.get("language");
            if (ObjectHelper.isEmpty(language)) {
                language = StringSupport.substringAfterLast(location, ":");
                language = StringSupport.substringAfterLast(language, ".");
            }
            if (ObjectHelper.isEmpty(language)) {
                throw new IllegalArgumentException("Unknown language " + language);
            }

            String name = (String) params.get("name");
            if (name == null) {
                name = StringSupport.substringAfter(location, ":");
                name = StringSupport.substringBeforeLast(name, ".");

                if (name.contains("/")) {
                    name = StringSupport.substringAfterLast(name, "/");
                }
            }

            this.location = location;
            this.name = name;
            this.language = language;
            this.loader = (String) params.get("loader");
            this.interceptors = (String) params.get("interceptors");
            this.compressed = Boolean.parseBoolean((String) params.get("compression"));
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getLanguage() {
            return language;
        }

        @Override
        public Optional<String> getLoader() {
            return Optional.ofNullable(loader);
        }

        @Override
        public List<String> getInterceptors() {
            return interceptors != null ? Arrays.asList(interceptors.split(",", -1)) : Collections.emptyList();
        }

        @Override
        public InputStream resolveAsInputStream(CamelContext ctx) {
            if (location == null) {
                throw new IllegalArgumentException("Cannot resolve null URI");
            }

            try {
                final ClassResolver cr = ctx.getClassResolver();
                final InputStream is = ResourceHelper.resolveResourceAsInputStream(cr, location);

                return compressed
                    ? new GZIPInputStream(Base64.getDecoder().wrap(is))
                    : is;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}

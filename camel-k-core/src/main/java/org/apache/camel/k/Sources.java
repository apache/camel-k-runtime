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
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

import org.apache.camel.CamelContext;
import org.apache.camel.k.support.StringSupport;
import org.apache.camel.support.ResourceHelper;
import org.apache.camel.util.ObjectHelper;

public final class  Sources {
    private Sources() {
    }

    public static Source fromURI(String uri) throws Exception {
        return fromDefinition(SourceDefinition.fromURI(uri));
    }

    public static Source fromBytes(String name, String language, String loader, List<String> interceptors, byte[] content) {
        return fromDefinition(SourceDefinition.fromBytes(null, name, language, loader, interceptors, content));
    }

    public static Source fromBytes(String name, String language, String loader, byte[] content) {
        return fromDefinition(SourceDefinition.fromBytes(null, name, language, loader, null, content));
    }

    public static Source fromBytes(String language, byte[] content) {
        return fromDefinition(SourceDefinition.fromBytes(null, UUID.randomUUID().toString(), language, null, null, content));
    }

    public static Source fromDefinition(SourceDefinition definition) {
        if (definition.getLocation() == null && definition.getContent() == null) {
            throw new IllegalArgumentException("Either the source location or the source content should be set");
        }

        return new Source() {
            @Override
            public String getId() {
                return ObjectHelper.supplyIfEmpty(definition.getId(), this::getName);
            }

            @Override
            public String getName() {
                String answer = definition.getName();
                if (ObjectHelper.isEmpty(answer) && ObjectHelper.isNotEmpty(definition.getLocation())) {
                    answer = StringSupport.substringAfter(definition.getLocation(), ":");
                    answer = StringSupport.substringBeforeLast(answer, ".");

                    if (answer.contains("/")) {
                        answer = StringSupport.substringAfterLast(answer, "/");
                    }
                }

                return answer;
            }

            @Override
            public String getLanguage() {
                String answer = definition.getLanguage();
                if (ObjectHelper.isEmpty(answer) && ObjectHelper.isNotEmpty(definition.getLocation())) {
                    answer = StringSupport.substringAfterLast(definition.getLocation(), ":");
                    answer = StringSupport.substringAfterLast(answer, ".");
                }

                return answer;
            }

            @Override
            public SourceType getType() {
                return ObjectHelper.supplyIfEmpty(definition.getType(), () -> SourceType.source);
            }

            @Override
            public Optional<String> getLoader() {
                return Optional.ofNullable(definition.getLoader());
            }

            @Override
            public List<String> getInterceptors() {
                return ObjectHelper.supplyIfEmpty(definition.getInterceptors(), Collections::emptyList);
            }

            @Override
            public List<String> getPropertyNames() {
                return ObjectHelper.supplyIfEmpty(definition.getPropertyNames(), Collections::emptyList);
            }

            /**
             * Read the content of the source as {@link InputStream}.
             *
             * @param ctx the {@link CamelContext}
             * @return the {@link InputStream} representing the source content
             */
            @Override
            public InputStream resolveAsInputStream(CamelContext ctx) {
                try {
                    InputStream is;

                    if (definition.getContent() != null) {
                        is = new ByteArrayInputStream(definition.getContent());
                    } else {
                        is = ResourceHelper.resolveMandatoryResourceAsInputStream(ctx, definition.getLocation());
                    }

                    return definition.isCompressed()
                        ? new GZIPInputStream(Base64.getDecoder().wrap(is))
                        : is;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
}

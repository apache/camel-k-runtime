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
package org.apache.camel.k.loader.yaml.spi;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.camel.CamelContext;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.Ordered;

public interface TypeResolver extends Ordered {
    String SERVICE_LOCATION = "META-INF/services/org/apache/camel/k/loader/yaml-parser/types";

    Class<?> resolve(CamelContext camelContext, String id);

    @Override
    default int getOrder() {
        return Ordered.LOWEST;
    }

    default Class<?> lookup(CamelContext camelContext, String id) {
        Set<TypeResolver> resolvers = new TreeSet<>(Comparator.comparingInt(Ordered::getOrder));
        resolvers.addAll(camelContext.getRegistry().findByType(TypeResolver.class));
        resolvers.add(this);

        for (TypeResolver resolver: resolvers) {
            Class<?> answer = resolver.resolve(camelContext, id);
            if (answer != null) {
                return answer;
            }
        }

        return camelContext.adapt(ExtendedCamelContext.class)
            .getFactoryFinder(SERVICE_LOCATION)
            .findClass("id")
            .orElseThrow(() -> new RuntimeException("No handler for step with id: " + id));
    }

    static TypeResolver caching(TypeResolver delegate) {
        final ConcurrentMap<String, Class<?>> cache = new ConcurrentHashMap<>();

        return new TypeResolver() {
            @Override
            public Class<?> resolve(CamelContext camelContext, String id) {
                return cache.computeIfAbsent(id, key -> delegate.resolve(camelContext, key));
            }
        };
    }
}

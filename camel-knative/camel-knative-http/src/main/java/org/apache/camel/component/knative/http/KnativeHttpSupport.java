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
package org.apache.camel.component.knative.http;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import io.vertx.core.http.HttpServerRequest;
import org.apache.camel.component.knative.spi.Knative;
import org.apache.camel.component.knative.spi.KnativeEnvironment;
import org.apache.camel.util.ObjectHelper;

public final  class KnativeHttpSupport {
    private KnativeHttpSupport() {
    }

    @SuppressWarnings("unchecked")
    public static void appendHeader(Map<String, Object> headers, String key, Object value) {
        if (headers.containsKey(key)) {
            Object existing = headers.get(key);
            List<Object> list;
            if (existing instanceof List) {
                list = (List<Object>) existing;
            } else {
                list = new ArrayList<>();
                list.add(existing);
            }
            list.add(value);
            value = list;
        }

        headers.put(key, value);
    }

    public static Predicate<HttpServerRequest> createFilter(KnativeEnvironment.KnativeServiceDefinition serviceDefinition) {
        Map<String, String> filters = serviceDefinition.getMetadata().entrySet().stream()
            .filter(e -> e.getKey().startsWith(Knative.KNATIVE_FILTER_PREFIX))
            .collect(Collectors.toMap(
                e -> e.getKey().substring(Knative.KNATIVE_FILTER_PREFIX.length()),
                e -> e.getValue()
            ));


        String path = ObjectHelper.supplyIfEmpty(serviceDefinition.getPath(), () -> KnativeHttp.DEFAULT_PATH);

        return v -> {
            if (!Objects.equals(path, v.path())) {
                return false;
            }
            if (filters.isEmpty()) {
                return true;
            }

            for (Map.Entry<String, String> entry : filters.entrySet()) {
                String ref = entry.getValue();
                String val = v.getHeader(entry.getKey());

                if (val == null) {
                    return false;
                }

                boolean matches = Objects.equals(ref, val) || val.matches(ref);
                if (!matches) {
                    return false;
                }
            }

            return true;
        };
    }
}

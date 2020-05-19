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
import org.apache.camel.AsyncCallback;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.component.knative.spi.CloudEvent;
import org.apache.camel.component.knative.spi.Knative;
import org.apache.camel.component.knative.spi.KnativeEnvironment;
import org.apache.camel.support.processor.DelegateAsyncProcessor;

public final class KnativeHttpSupport {
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

        return v -> {
            if (filters.isEmpty()) {
                return true;
            }

            for (Map.Entry<String, String> entry : filters.entrySet()) {
                final List<String> values = v.headers().getAll(entry.getKey());
                final String ref = entry.getValue();

                if (values.isEmpty()) {
                    return false;
                }

                String val = values.get(values.size() - 1);
                int idx = val.lastIndexOf(',');

                if (values.size() == 1 && idx != -1) {
                    val = val.substring(idx + 1);
                    val = val.trim();
                }

                boolean matches = Objects.equals(ref, val) || val.matches(ref);
                if (!matches) {
                    return false;
                }
            }

            return true;
        };
    }

    /**
     * Removes cloud event headers at the end of the processing.
     */
    public static Processor withoutCloudEventHeaders(Processor delegate, CloudEvent ce) {
        return new DelegateAsyncProcessor(delegate) {
            @Override
            public boolean process(Exchange exchange, AsyncCallback callback) {
                return processor.process(exchange, doneSync -> {
                    final Message message = exchange.getMessage();

                    // remove CloudEvent headers
                    for (CloudEvent.Attribute attr : ce.attributes()) {
                        message.removeHeader(attr.http());
                    }

                    callback.done(doneSync);
                });
            }
        };
    }

    /**
     * Remap camel headers to cloud event http headers.
     */
    public static Processor remapCloudEventHeaders(Processor delegate, CloudEvent ce) {
        return new DelegateAsyncProcessor(delegate) {
            @Override
            public boolean process(Exchange exchange, AsyncCallback callback) {
                return processor.process(exchange, doneSync -> {
                    final Message message = exchange.getMessage();

                    // remap CloudEvent camel --> http
                    for (CloudEvent.Attribute attr : ce.attributes()) {
                        Object value = message.getHeader(attr.id());
                        if (value != null) {
                            message.setHeader(attr.http(), value);
                        }
                    }

                    callback.done(doneSync);
                });
            }
        };
    }

}

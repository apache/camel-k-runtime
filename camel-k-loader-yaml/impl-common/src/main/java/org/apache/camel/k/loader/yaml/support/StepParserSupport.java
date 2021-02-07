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
package org.apache.camel.k.loader.yaml.support;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.camel.CamelContext;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.k.loader.yaml.model.Step;
import org.apache.camel.k.loader.yaml.spi.ProcessorStepParser;
import org.apache.camel.k.loader.yaml.spi.StepParser;
import org.apache.camel.k.loader.yaml.spi.StepParserException;
import org.apache.camel.model.OutputNode;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.spi.EndpointUriFactory;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.StringHelper;
import org.apache.camel.util.URISupport;

public final class StepParserSupport {
    private StepParserSupport() {
    }

    public static <T> T notNull(T value, String name) {
        if (value == null) {
            throw new StepParserException(name + " must be specified", name);
        }

        return value;
    }

    public static ProcessorDefinition<?> convertSteps(StepParser.Context context, ProcessorDefinition<?> parent, List<Step> steps) {
        ObjectHelper.notNull(context, "step context");
        ObjectHelper.notNull(parent, "parent");

        if (steps == null) {
            return parent;
        }

        ProcessorDefinition<?> current = parent;

        for (Step step : steps) {
            ProcessorDefinition<?> child = ProcessorStepParser.invoke(
                ProcessorStepParser.Context.of(context, current, step.node),
                step.id
            );

            current.addOutput(child);

            if (child instanceof OutputNode && child.getOutputs().isEmpty()) {
                current = child;
            }
        }

        return parent;
    }

    public static String createEndpointUri(CamelContext context, String uri, Map<String, Object> parameters) {
        String answer = uri;

        if (parameters == null || parameters.isEmpty()) {
            //
            // nothing to do here, there are no parameters so we can return the
            // uri as it is.
            //
            return answer;
        }
        if (uri.indexOf('?') != -1) {
            //
            // we support URIs defined as scheme only or scheme and path params,
            // query params are not supported so a definition like:
            //
            // - from:
            //     uri: "foo:bar?option1=value1"
            //     parameters:
            //         option2: value2
            //
            // is not supported and leads to the an IllegalArgumentException being
            // thrown.
            //
            throw new IllegalArgumentException("Uri should not contains query params (uri: " + uri + ")");
        }

        final String scheme = uri.contains(":") ? StringHelper.before(uri, ":") : uri;
        final EndpointUriFactory factory = context.adapt(ExtendedCamelContext.class).getEndpointUriFactory(scheme);

        try {
            if (factory != null && factory.isEnabled(scheme)) {
                if (scheme.equals(uri)) {
                    //
                    // if the uri is expressed as simple scheme, then we can use the
                    // discovered EndpointUriFactory to build the uri
                    //
                    answer = factory.buildUri(scheme, parameters, false);
                } else {
                    //
                    // otherwise we have to compose it but we can still leverage the
                    // discovered EndpointUriFactory to properly handle secrets
                    //
                    Map<String, Object> options = new TreeMap<>(parameters);

                    for (String secretParameter : factory.secretPropertyNames()) {
                        Object val = options.get(secretParameter);
                        if (val instanceof String) {
                            String newVal = (String) val;
                            if (!newVal.startsWith("#") && !newVal.startsWith("RAW(")) {
                                options.put(secretParameter, "RAW(" + val + ")");
                            }
                        }
                    }

                    answer += "?" + URISupport.createQueryString(options, false);
                }
            } else {
                answer += "?" + URISupport.createQueryString(parameters, false);
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }

        return answer;
    }
}

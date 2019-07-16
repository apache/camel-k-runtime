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
package org.apache.camel.k.loader.yaml.parser;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.camel.CamelContext;
import org.apache.camel.k.loader.yaml.model.Step;
import org.apache.camel.model.OutputNode;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.support.IntrospectionSupport;
import org.apache.camel.support.PropertyBindingSupport;
import org.apache.camel.util.ObjectHelper;

public final class StepParserSupport {
    private StepParserSupport() {
    }

    @SuppressWarnings("unchecked")
    public static <T extends ProcessorDefinition<?>, I> T adaptProcessorToSuper(CamelContext context, I instance) {
        ObjectHelper.notNull(context, "camel context");
        ObjectHelper.notNull(instance, "instance");

        return adaptProcessor(context, (Class<T>)instance.getClass().getSuperclass(), instance);
    }

    @SuppressWarnings("unchecked")
    public static <T extends ProcessorDefinition<?>, I> T adaptProcessor(CamelContext context, Class<T> type, I instance) {
        ObjectHelper.notNull(context, "camel context");
        ObjectHelper.notNull(type, "type");
        ObjectHelper.notNull(instance, "instance");

        if (Objects.equals(type, instance.getClass())) {
            return (T)instance;
        }

        final T answer = context.getInjector().newInstance(type);
        final Map<String, Object> properties = IntrospectionSupport.getNonNullProperties(instance);

        PropertyBindingSupport.bindProperties(context, answer, properties);

        return answer;
    }

    public static <T> T notNull(T value, String name) {
        if (value == null) {
            throw new StepParserException(name + " must be specified", name);
        }

        return value;
    }

    public static ProcessorDefinition<?> convertSteps(ProcessorStepParser.Context context, ProcessorDefinition<?> parent, List<Step> steps) {
        ObjectHelper.notNull(context, "step context");
        ObjectHelper.notNull(parent, "parent");

        if (steps == null) {
            return parent;
        }

        ProcessorDefinition<?> current = parent;

        for (Step step : steps) {
            ProcessorDefinition<?> child = ProcessorStepParser.invoke(
                ProcessorStepParser.Context.of(context, step.node),
                step.id
            );

            current.addOutput(child);

            if (child instanceof OutputNode && child.getOutputs().isEmpty()) {
                current = child;
            }
        }

        return parent;
    }
}

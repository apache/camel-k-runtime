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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.PropertyBindingException;
import org.apache.camel.k.annotation.yaml.YAMLNodeDefinition;
import org.apache.camel.k.annotation.yaml.YAMLStepParser;
import org.apache.camel.k.loader.yaml.spi.StartStepParser;
import org.apache.camel.spi.PropertyConfigurer;
import org.apache.camel.support.PropertyBindingSupport;
import org.apache.camel.support.service.ServiceHelper;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.StringHelper;


@YAMLStepParser(id = "beans", definition = BeansStepParser.Definition.class, schema = false)
public class BeansStepParser implements StartStepParser {
    @Override
    public Object process(Context context) {
        final List<Definition> beans = context.node(new TypeReference<>() {});
        final CamelContext camelContext = context.getCamelContext();

        for (Definition bean: beans) {
            ObjectHelper.notNull(bean.name, "bean name");
            ObjectHelper.notNull(bean.type, "bean type");

            final Map<String, Object> properties;

            if (bean.properties != null) {
                properties = new HashMap<>();
                bean.properties.forEach((k, v) -> properties.put(StringHelper.dashToCamelCase(k), v));
            } else {
                properties = Collections.emptyMap();
            }

            try {
                String type = bean.type;
                if (!type.startsWith("#class:")) {
                    type = "#class:" + type;
                }

                Object target = PropertyBindingSupport.resolveBean(camelContext, null, type);

                setPropertiesOnTarget(camelContext, target, properties);
                camelContext.getRegistry().bind(bean.name, target);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return null;
    }

    @YAMLNodeDefinition
    public static final class Definition {
        @JsonProperty(required = true)
        public String name;
        @JsonProperty(required = true)
        public String type;
        @JsonProperty
        public Map<String, Object> properties;
    }

    protected static void setPropertiesOnTarget(
            CamelContext context,
            Object target,
            Map<String, Object> properties) {

        ObjectHelper.notNull(context, "context");
        ObjectHelper.notNull(target, "target");
        ObjectHelper.notNull(properties, "properties");

        PropertyConfigurer configurer = null;
        if (target instanceof Component) {
            // the component needs to be initialized to have the configurer ready
            ServiceHelper.initService(target);
            configurer = ((Component) target).getComponentPropertyConfigurer();
        }

        if (configurer == null) {
            String name = target.getClass().getSimpleName();
            if (target instanceof ExtendedCamelContext) {
                // special for camel context itself as we have an extended configurer
                name = "ExtendedCamelContext";
            }

            // see if there is a configurer for it
            configurer = context.adapt(ExtendedCamelContext.class)
                .getConfigurerResolver()
                .resolvePropertyConfigurer(name, context);
        }

        try {
            PropertyBindingSupport.build()
                .withMandatory(true)
                .withRemoveParameters(false)
                .withConfigurer(configurer)
                .withIgnoreCase(true)
                .withFlattenProperties(true)
                .bind(context, target, properties);
        } catch (PropertyBindingException e) {
            String key = e.getOptionKey();
            if (key == null) {
                String prefix = e.getOptionPrefix();
                if (prefix != null && !prefix.endsWith(".")) {
                    prefix = "." + prefix;
                }

                key = prefix != null
                    ? prefix + "." + e.getPropertyName()
                    : e.getPropertyName();
            }

            // enrich the error with more precise details with option prefix and key
            throw new PropertyBindingException(e.getTarget(), e.getPropertyName(), e.getValue(), null, key, e.getCause());
        }
    }
}

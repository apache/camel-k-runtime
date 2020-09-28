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
package org.apache.camel.component.kamelet;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Predicate;

import org.apache.camel.CamelContext;
import org.apache.camel.spi.PropertiesComponent;
import org.apache.camel.util.StringHelper;

public final class Kamelet {
    public static final String PROPERTIES_PREFIX = "camel.kamelet.";
    public static final String SCHEME = "kamelet";
    public static final String SOURCE_ID = "source";
    public static final String SINK_ID = "sink";

    private Kamelet() {
    }

    public static Predicate<String> startsWith(String prefix) {
        return item -> item.startsWith(prefix);
    }

    public static String extractTemplateId(CamelContext context, String remaining) {
        if (SOURCE_ID.equals(remaining) || SINK_ID.equals(remaining)) {
            return context.resolvePropertyPlaceholders("{{templateId}}");
        }

        String answer = StringHelper.before(remaining, "/");
        if (answer == null) {
            answer = remaining;
        }

        return answer;
    }

    public static String extractRouteId(CamelContext context, String remaining) {
        if (SOURCE_ID.equals(remaining) || SINK_ID.equals(remaining)) {
            return context.resolvePropertyPlaceholders("{{routeId}}");
        }

        String answer = StringHelper.after(remaining, "/");
        if (answer == null) {
            answer = extractTemplateId(context, remaining) + "-" + context.getUuidGenerator().generateUuid();
        }

        return answer;
    }

    public static Map<String, Object> extractKameletProperties(CamelContext context, String... elements) {
        PropertiesComponent pc = context.getPropertiesComponent();
        Map<String, Object> properties = new HashMap<>();
        String prefix = Kamelet.PROPERTIES_PREFIX;

        for (String element: elements) {
            if (element == null) {
                continue;
            }

            prefix = prefix + element + ".";

            Properties prefixed = pc.loadProperties(Kamelet.startsWith(prefix));
            for (String name : prefixed.stringPropertyNames()) {
                properties.put(name.substring(prefix.length()), prefixed.getProperty(name));
            }
        }

        return properties;
    }
}

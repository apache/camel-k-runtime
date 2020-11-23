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

import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.PropertyBindingSupport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class KameletComponentTest {
    @Test
    public void testComponent()  {
        CamelContext context = new DefaultCamelContext();
        KameletComponent component = context.getComponent(Kamelet.SCHEME, KameletComponent.class);

        PropertyBindingSupport.build()
            .withIgnoreCase(true)
            .withReflection(false)
            .withRemoveParameters(false)
            .withCamelContext(context)
            .withTarget(component)
            .withConfigurer(component.getComponentPropertyConfigurer())
            .withProperties(Map.of(
                "configuration.template-properties[myTemplate].foo", "bar",
                "configuration.route-properties[myRoute].foo", "baz"
            ))
            .bind();


        assertThat(component.getConfiguration().getTemplateProperties()).isNotEmpty();
        assertThat(component.getConfiguration().getTemplateProperties()).containsKey("myTemplate");
        assertThat(component.getConfiguration().getTemplateProperties().get("myTemplate")).containsEntry("foo", "bar");
        assertThat(component.getConfiguration().getRouteProperties()).isNotEmpty();
        assertThat(component.getConfiguration().getRouteProperties()).containsKey("myRoute");
        assertThat(component.getConfiguration().getRouteProperties().get("myRoute")).containsEntry("foo", "baz");
    }
}

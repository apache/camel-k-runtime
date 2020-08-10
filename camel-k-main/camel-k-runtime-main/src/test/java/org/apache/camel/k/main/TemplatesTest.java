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
package org.apache.camel.k.main;

import java.util.Map;

import org.apache.camel.Route;
import org.apache.camel.k.SourceType;
import org.apache.camel.k.main.support.RuntimeTestSupport;
import org.apache.camel.model.ModelCamelContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TemplatesTest {
    @Test
    public void templatesCanBeLoadedAndMaterialized() throws Exception {
        RuntimeTestSupport.run(
            Map.of(
                "camel.k.sources[0].id", "my-template",
                "camel.k.sources[0].location", "classpath:MyRouteTemplate.java",
                "camel.k.sources[0].type", SourceType.template.name(),
                "camel.k.sources[0].property-names[0]", "message"
            ),
            runtime -> {
                var context = runtime.getCamelContext(ModelCamelContext.class);

                assertThat(context.getRouteTemplateDefinitions()).hasSize(1);
                assertThat(context.getRouteDefinitions()).isEmpty();

                context.addRouteFromTemplate("myRoute", "my-template", Map.of("message", "test"));

                assertThat(context.getRoutes())
                    .hasSize(1)
                    .first().hasFieldOrPropertyWithValue("id", "myRoute");
            }
        );
    }

    @Test
    public void templatesCanBeLoadedAndMaterializedByKamelets() throws Exception {
        RuntimeTestSupport.run(
            Map.of(
                // template
                "camel.k.sources[0].id", "my-template",
                "camel.k.sources[0].location", "classpath:MyRouteTemplate.java",
                "camel.k.sources[0].type", SourceType.template.name(),
                "camel.k.sources[0].property-names[0]", "message",
                // route
                "camel.k.sources[1].location", "classpath:MyRoutesWithKamelets.java",
                "camel.k.sources[1].type", SourceType.source.name(),
                // props
                "camel.kamelet.my-template.message", "default-message"
            ),
            runtime -> {
                var context = runtime.getCamelContext(ModelCamelContext.class);

                // templates
                assertThat(context.getRouteTemplateDefinitions()).hasSize(1);

                // 2 routes defined in MyRoutesWithKamelets
                // 2 routes materialized from templates by camel-kamelet
                assertThat(context.getRouteDefinitions()).hasSize(4);

                assertThat(context.getRoutes())
                    .hasSize(4)
                    .extracting(Route::getId)
                    .containsExactlyInAnyOrder(
                        "k1", "k2",  // routes from MyRoutesWithKamelets
                        "myKamelet1", "myKamelet2" // routes from templates
                    );
            }
        );
    }
}

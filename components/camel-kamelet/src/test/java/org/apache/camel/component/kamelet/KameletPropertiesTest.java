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

import java.util.Properties;

import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.apache.http.annotation.Obsolete;
import org.junit.jupiter.api.Test;

import static org.apache.camel.k.test.CamelKTestSupport.asProperties;
import static org.assertj.core.api.Assertions.assertThat;

public class KameletPropertiesTest extends CamelTestSupport {
    @Test
    public void propertiesAreTakenFromRouteId() throws Exception {
        assertThat(
            fluentTemplate
                .to("kamelet:setBody/test")
                .request(String.class)
        ).isEqualTo("from-route");
    }

    @Test
    public void propertiesAreTakenFromTemplateId() throws Exception {
        assertThat(
            fluentTemplate
                .to("kamelet:setBody")
                .request(String.class)
        ).isEqualTo("from-template");
    }

    @Test
    public void propertiesAreTakenFromURI() {
        assertThat(
            fluentTemplate
                .to("kamelet:setBody?bodyValue={{bodyValue}}")
                .request(String.class)
        ).isEqualTo("from-uri");
    }

    // **********************************************
    //
    // test set-up
    //
    // **********************************************

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        return asProperties(
            "bodyValue", "from-uri",
            Kamelet.PROPERTIES_PREFIX + "setBody.bodyValue", "from-template",
            Kamelet.PROPERTIES_PREFIX + "setBody.test.bodyValue", "from-route"
        );
    }

    @Obsolete
    protected RoutesBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                // template
                routeTemplate("setBody")
                    .templateParameter("bodyValue")
                    .from("kamelet:source")
                    .setBody().constant("{{bodyValue}}");
            }
        };
    }
}

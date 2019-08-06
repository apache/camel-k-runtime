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
package org.apache.camel.k.loader.xml;

import java.util.List;
import java.util.stream.Stream;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.k.RoutesLoader;
import org.apache.camel.k.Source;
import org.apache.camel.k.Sources;
import org.apache.camel.k.support.RuntimeSupport;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.ToDefinition;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

public class RoutesLoaderTest {
    @ParameterizedTest
    @MethodSource("parameters")
    public void testLoaders(String location, Class<? extends RoutesLoader> type) throws Exception {
        Source source = Sources.fromURI(location);
        RoutesLoader loader = RuntimeSupport.loaderFor(new DefaultCamelContext(), source);
        RouteBuilder builder = loader.load(new DefaultCamelContext(), source);

        assertThat(loader).isInstanceOf(type);
        assertThat(builder).isNotNull();

        builder.setContext(new DefaultCamelContext());
        builder.configure();

        List<RouteDefinition> routes = builder.getRouteCollection().getRoutes();
        assertThat(routes).hasSize(1);
        assertThat(routes.get(0).getInput().getEndpointUri()).isEqualTo("timer:tick");
        assertThat(routes.get(0).getOutputs().get(0)).isInstanceOf(ToDefinition.class);
    }

    static Stream<Arguments> parameters() {
        return Stream.of(
            Arguments.arguments("classpath:routes.xml", XmlRoutesLoader.class)
        );
    }
}

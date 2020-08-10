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

import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.Route;
import org.apache.camel.component.knative.KnativeComponent;
import org.apache.camel.component.knative.spi.Knative;
import org.apache.camel.component.knative.spi.KnativeEnvironment;
import org.apache.camel.component.knative.test.KnativeEnvironmentSupport;
import org.apache.camel.k.Runtime;
import org.apache.camel.k.http.PlatformHttpServiceContextCustomizer;
import org.apache.camel.k.listener.ContextConfigurer;
import org.apache.camel.k.listener.SourcesConfigurer;
import org.apache.camel.k.main.support.MyBean;
import org.apache.camel.k.support.SourcesSupport;
import org.apache.camel.k.test.AvailablePortFinder;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.ToDefinition;
import org.apache.camel.util.ObjectHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.camel.util.CollectionHelper.mapOf;
import static org.assertj.core.api.Assertions.assertThat;

public class RuntimeTest {
    private ApplicationRuntime runtime;

    @BeforeEach
    public void setUp() {
        runtime = new ApplicationRuntime();
    }

    @AfterEach
    public void cleanUp() throws Exception {
        if (runtime != null) {
            runtime.stop();
        }
    }

    @Test
    void testLoadMultipleRoutes() throws Exception {
        runtime.addListener(new ContextConfigurer());
        runtime.addListener(SourcesSupport.forRoutes("classpath:r1.js", "classpath:r2.mytype?language=js"));
        runtime.addListener(Runtime.Phase.Started, r -> {
            CamelContext context = r.getCamelContext();
            List<Route> routes = context.getRoutes();

            assertThat(routes).hasSize(2);
            assertThat(routes).anyMatch(p -> ObjectHelper.equal("r1", p.getId()));
            assertThat(routes).anyMatch(p -> ObjectHelper.equal("r2", p.getId()));

            runtime.stop();
        });

        runtime.run();
    }

    @Test
    void testLoadRouteAndRest() throws Exception {
        runtime.addListener(new ContextConfigurer());
        runtime.addListener(SourcesSupport.forRoutes("classpath:routes.xml", "classpath:rests.xml"));
        runtime.addListener(Runtime.Phase.Started, r -> {
            ModelCamelContext context = r.getCamelContext(ModelCamelContext.class);

            assertThat(context.getRouteDefinitions()).isNotEmpty();
            assertThat(context.getRestDefinitions()).isNotEmpty();

            runtime.stop();
        });

        runtime.run();
    }

    @Test
    void testLoadRouteWithExpression() throws Exception {
        runtime.setProperties(mapOf(
            "the.body", "10"
        ));

        runtime.addListener(new ContextConfigurer());
        runtime.addListener(SourcesSupport.forRoutes("classpath:routes-with-expression.xml"));
        runtime.addListener(Runtime.Phase.Started, Runtime::stop);
        runtime.run();
    }

    @Test
    public void testLoadJavaSource() throws Exception {
        runtime.addListener(SourcesSupport.forRoutes("classpath:MyRoutesWithBeans.java", "classpath:MyRoutesConfig.java"));
        runtime.addListener(Runtime.Phase.Started, r -> {
            assertThat(runtime.getCamelContext().getRoutes()).hasSize(1);
            assertThat(runtime.getRegistry().lookupByName("my-processor")).isNotNull();
            assertThat(runtime.getRegistry().lookupByName("my-bean")).isInstanceOfSatisfying(MyBean.class, b -> {
                assertThat(b).hasFieldOrPropertyWithValue("name", "my-bean-name");
            });
            r.stop();
        });
        runtime.run();
    }

    @Test
    public void testLoadJavaSourceFromProperties() throws Exception {
        runtime.setInitialProperties(
            "camel.k.sources[0].name", "MyRoutesWithBeans",
            "camel.k.sources[0].location", "classpath:MyRoutesWithBeans.java",
            "camel.k.sources[0].language", "java",
            "camel.k.sources[1].name", "MyRoutesConfig",
            "camel.k.sources[1].location", "classpath:MyRoutesConfig.java",
            "camel.k.sources[1].language", "java"
        );
        runtime.addListener(new SourcesConfigurer());
        runtime.addListener(Runtime.Phase.Started, r -> {
            assertThat(runtime.getCamelContext().getRoutes()).hasSize(1);
            assertThat(runtime.getRegistry().lookupByName("my-processor")).isNotNull();
            assertThat(runtime.getRegistry().lookupByName("my-bean")).isInstanceOfSatisfying(MyBean.class, b -> {
                assertThat(b).hasFieldOrPropertyWithValue("name", "my-bean-name");
            });
            r.stop();
        });
        runtime.run();
    }

    @Test
    public void testLoadJavaSourceFromSimpleProperties() throws Exception {
        runtime.setInitialProperties(
            "camel.k.sources[0].location", "classpath:MyRoutesWithBeans.java",
            "camel.k.sources[1].location", "classpath:MyRoutesConfig.java"
        );
        runtime.addListener(new SourcesConfigurer());
        runtime.addListener(Runtime.Phase.Started, Runtime::stop);
        runtime.run();

        assertThat(runtime.getRegistry().lookupByName("my-processor")).isNotNull();
        assertThat(runtime.getRegistry().lookupByName("my-bean")).isInstanceOfSatisfying(MyBean.class, b -> {
            assertThat(b).hasFieldOrPropertyWithValue("name", "my-bean-name");
        });
    }

    @Test
    public void testLoadJavaSourceWrap() throws Exception {
        KnativeComponent component = new KnativeComponent();
        component.setEnvironment(KnativeEnvironment.on(
            KnativeEnvironmentSupport.endpoint(Knative.EndpointKind.sink, "sink", "localhost", AvailablePortFinder.getNextAvailable())
        ));

        PlatformHttpServiceContextCustomizer phsc = new PlatformHttpServiceContextCustomizer();
        phsc.setBindPort(AvailablePortFinder.getNextAvailable());
        phsc.apply(runtime.getCamelContext());

        runtime.getCamelContext().addComponent("knative", component);
        runtime.addListener(SourcesSupport.forRoutes("classpath:MyRoutesWithBeans.java?interceptors=knative-source"));
        runtime.addListener(Runtime.Phase.Started, Runtime::stop);
        runtime.run();

        assertThat(runtime.getRegistry().lookupByName("my-bean")).isInstanceOfSatisfying(MyBean.class, b -> {
            assertThat(b).hasFieldOrPropertyWithValue("name", "my-bean-name");
        });
        assertThat(runtime.getCamelContext(ModelCamelContext.class).getRouteDefinition("my-route")).satisfies(definition -> {
            assertThat(definition.getOutputs()).last().isInstanceOfSatisfying(ToDefinition.class, to -> {
                assertThat(to.getEndpointUri()).isEqualTo("knative://endpoint/sink");
            });
        });
    }

}

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
import org.apache.camel.k.Runtime;
import org.apache.camel.k.listener.ContextConfigurer;
import org.apache.camel.k.listener.RoutesConfigurer;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.util.CollectionHelper;
import org.apache.camel.util.ObjectHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        runtime.addListener(RoutesConfigurer.forRoutes("classpath:r1.js", "classpath:r2.mytype?language=js"));
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
        runtime.addListener(RoutesConfigurer.forRoutes("classpath:routes.xml", "classpath:rests.xml"));
        runtime.addListener(Runtime.Phase.Started, r -> {
            CamelContext context = r.getCamelContext();

            assertThat(context.adapt(ModelCamelContext.class).getRouteDefinitions()).isNotEmpty();
            assertThat(context.adapt(ModelCamelContext.class).getRestDefinitions()).isNotEmpty();

            runtime.stop();
        });

        runtime.run();
    }

    @Test
    void testLoadRouteWithExpression() throws Exception {
        runtime.setProperties(CollectionHelper.mapOf(
            "the.body", "10"
        ));

        runtime.addListener(new ContextConfigurer());
        runtime.addListener(RoutesConfigurer.forRoutes("classpath:routes-with-expression.xml"));
        runtime.addListener(Runtime.Phase.Started, r -> runtime.stop());
        runtime.run();
    }


    @Test
    public void testLoadJavaClassWithBeans() throws Exception {
        ApplicationRuntime runtime = new ApplicationRuntime();
        runtime.addListener(RoutesConfigurer.forRoutes("classpath:" + MyRoutesWithBeans.class.getName() + ".class"));
        runtime.addListener(Runtime.Phase.Started, r ->  runtime.stop());
        runtime.run();

        assertThat(runtime.getRegistry().lookupByName("my-bean")).isInstanceOfSatisfying(MyBean.class, b -> {
            assertThat(b).hasFieldOrPropertyWithValue("name", "my-bean-name");
        });
    }

    @Test
    public void testLoadJavaSourceWithBeans() throws Exception {
        ApplicationRuntime runtime = new ApplicationRuntime();
        runtime.addListener(RoutesConfigurer.forRoutes("classpath:MyRoutesWithBeans.java"));
        runtime.addListener(Runtime.Phase.Started, r ->  runtime.stop());
        runtime.run();

        assertThat(runtime.getRegistry().lookupByName("my-bean")).isInstanceOfSatisfying(MyBean.class, b -> {
            assertThat(b).hasFieldOrPropertyWithValue("name", "my-bean-name");
        });
    }
}

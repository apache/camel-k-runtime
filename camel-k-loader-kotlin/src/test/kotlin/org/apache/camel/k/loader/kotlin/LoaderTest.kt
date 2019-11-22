/**
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
package org.apache.camel.k.loader.kotlin

import org.apache.camel.CamelContext
import org.apache.camel.RoutesBuilder
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.k.Runtime
import org.apache.camel.k.Sources
import org.apache.camel.k.support.RuntimeSupport
import org.apache.camel.model.ProcessDefinition
import org.apache.camel.model.ToDefinition
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.*

class LoaderTest {

    @Test
    fun `load routes`() {
        var runtime = TestRuntime()
        var source = Sources.fromURI("classpath:routes.kts")
        val loader = RuntimeSupport.loaderFor(runtime.camelContext, source)

        loader.load(runtime, source)

        assertThat(loader).isInstanceOf(KotlinSourceLoader::class.java)
        assertThat(runtime.builders).hasSize(1)
        assertThat(runtime.builders[0]).isInstanceOf(RouteBuilder::class.java)

        var builder = runtime.builders[0] as RouteBuilder
        builder.context = runtime.camelContext
        builder.configure()

        val routes = builder.routeCollection.routes
        assertThat(routes).hasSize(1)
        assertThat(routes[0].input.endpointUri).isEqualTo("timer:tick")
        assertThat(routes[0].outputs[0]).isInstanceOf(ProcessDefinition::class.java)
        assertThat(routes[0].outputs[1]).isInstanceOf(ToDefinition::class.java)
    }

    @Test
    fun `load routes with endpoint dsl`() {
        var runtime = TestRuntime()
        var source = Sources.fromURI("classpath:routes-with-endpoint-dsl.kts")
        val loader = RuntimeSupport.loaderFor(runtime.camelContext, source)

        loader.load(runtime, source)

        assertThat(loader).isInstanceOf(KotlinSourceLoader::class.java)
        assertThat(runtime.builders).hasSize(1)
        assertThat(runtime.builders[0]).isInstanceOf(RouteBuilder::class.java)

        var builder = runtime.builders[0] as RouteBuilder
        builder.context = runtime.camelContext
        builder.configure()

        val routes = builder.routeCollection.routes
        assertThat(routes).hasSize(1)
        assertThat(routes[0].input.endpointUri).isEqualTo("timer:tick?period=1s")
        assertThat(routes[0].outputs[0]).isInstanceOfSatisfying(ToDefinition::class.java) {
            assertThat(it.endpointUri).isEqualTo("log:info")
        }
    }

    internal class TestRuntime : Runtime {
        private val context: CamelContext
        val builders: MutableList<RoutesBuilder>

        init {
            this.context = DefaultCamelContext()
            this.builders = ArrayList()
        }

        override fun getCamelContext(): CamelContext {
            return this.context
        }

        override fun addRoutes(builder: RoutesBuilder) {
            this.builders.add(builder)
        }
    }
}

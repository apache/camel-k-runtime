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
package org.apache.camel.k.loader.kotlin.dsl

import org.apache.camel.Processor
import org.apache.camel.component.log.LogComponent
import org.apache.camel.component.seda.SedaComponent
import org.apache.camel.k.Runtime
import org.apache.camel.k.jvm.ApplicationRuntime
import org.apache.camel.k.listener.RoutesConfigurer
import org.apache.camel.model.ModelCamelContext
import org.apache.camel.processor.FatalFallbackErrorHandler
import org.apache.camel.spi.ExchangeFormatter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class IntegrationTest {
    @Test
    fun `load integration with rest`() {
        var runtime = ApplicationRuntime()
        runtime.addListener(RoutesConfigurer.forRoutes("classpath:routes-with-rest.kts"))
        runtime.addListener(Runtime.Phase.Started) { runtime.stop() }
        runtime.run()

        assertThat(runtime.camelContext.restConfiguration.host).isEqualTo("my-host")
        assertThat(runtime.camelContext.restConfiguration.port).isEqualTo(9192)
        assertThat(runtime.camelContext.getRestConfiguration("undertow", false).host).isEqualTo("my-undertow-host")
        assertThat(runtime.camelContext.getRestConfiguration("undertow", false).port).isEqualTo(9193)
        assertThat(runtime.camelContext.adapt(ModelCamelContext::class.java).restDefinitions.size).isEqualTo(1)
        assertThat(runtime.camelContext.adapt(ModelCamelContext::class.java).restDefinitions[0].path).isEqualTo("/my/path")
    }

    @Test
    fun `load integration with binding`() {
        var runtime = ApplicationRuntime()
        runtime.addListener(RoutesConfigurer.forRoutes("classpath:routes-with-bindings.kts"))
        runtime.addListener(Runtime.Phase.Started) { runtime.stop() }
        runtime.run()

        assertThat(runtime.camelContext.registry.lookupByName("my-entry")).isEqualTo("myRegistryEntry1")
        assertThat(runtime.camelContext.registry.lookupByName("my-proc")).isInstanceOf(Processor::class.java)
    }

    @Test
    fun `load integration with component configuration`() {
        val sedaSize = AtomicInteger()
        val sedaConsumers = AtomicInteger()
        val mySedaSize = AtomicInteger()
        val mySedaConsumers = AtomicInteger()
        val format = AtomicReference<ExchangeFormatter>()

        var runtime = ApplicationRuntime()
        runtime.addListener(RoutesConfigurer.forRoutes("classpath:routes-with-component-configuration.kts"))
        runtime.addListener(Runtime.Phase.Started) {
            val seda = runtime.camelContext.getComponent("seda", SedaComponent::class.java)
            val mySeda = runtime.camelContext.getComponent("mySeda", SedaComponent::class.java)
            val log = runtime.camelContext.getComponent("log", LogComponent::class.java)

            sedaSize.set(seda!!.queueSize)
            sedaConsumers.set(seda.concurrentConsumers)
            mySedaSize.set(mySeda!!.queueSize)
            mySedaConsumers.set(mySeda.concurrentConsumers)
            format.set(log!!.exchangeFormatter)

            runtime.stop()
        }

        runtime.run()

        assertThat(sedaSize.get()).isEqualTo(1234)
        assertThat(sedaConsumers.get()).isEqualTo(12)
        assertThat(mySedaSize.get()).isEqualTo(4321)
        assertThat(mySedaConsumers.get()).isEqualTo(21)
        assertThat(format.get()).isNotNull
    }

    @Test
    fun `load integration with error handler`() {
        var onExceptions = mutableListOf<Processor>()

        var runtime = ApplicationRuntime()
        runtime.addListener(RoutesConfigurer.forRoutes("classpath:routes-with-error-handler.kts"))
        runtime.addListener(Runtime.Phase.Started) {
            assertThat(it.camelContext.routes).hasSize(1)
            assertThat(it.camelContext.routes[0].routeContext.getOnException("my-on-exception")).isNotNull

            onExceptions.add(it.camelContext.routes[0].routeContext.getOnException("my-on-exception"))

            runtime.stop()
        }

        runtime.run()

        assertThat(onExceptions).hasSize(1)
        assertThat(onExceptions).first().isInstanceOf(FatalFallbackErrorHandler::class.java)
    }
}
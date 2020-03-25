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

import org.apache.camel.Predicate
import org.apache.camel.Processor
import org.apache.camel.RuntimeCamelException
import org.apache.camel.component.jackson.JacksonDataFormat
import org.apache.camel.component.log.LogComponent
import org.apache.camel.component.seda.SedaComponent
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.k.Runtime
import org.apache.camel.k.listener.RoutesConfigurer.forRoutes
import org.apache.camel.language.bean.BeanLanguage
import org.apache.camel.model.ModelCamelContext
import org.apache.camel.model.rest.GetVerbDefinition
import org.apache.camel.model.rest.PostVerbDefinition
import org.apache.camel.processor.FatalFallbackErrorHandler
import org.apache.camel.support.DefaultHeaderFilterStrategy
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.IllegalArgumentException
import java.lang.RuntimeException
import javax.sql.DataSource

class IntegrationTest {
    @Test
    fun `load integration with rest`() {
        val context = DefaultCamelContext()
        val runtime = Runtime.on(context)

        forRoutes("classpath:routes-with-rest.kts").accept(Runtime.Phase.ConfigureRoutes, runtime)

        assertThat(context.restConfiguration.host).isEqualTo("my-host")
        assertThat(context.restConfiguration.port).isEqualTo(9192)
        assertThat(context.getRestConfiguration("undertow", false).host).isEqualTo("my-undertow-host")
        assertThat(context.getRestConfiguration("undertow", false).port).isEqualTo(9193)
        assertThat(context.adapt(ModelCamelContext::class.java).restDefinitions.size).isEqualTo(2)

        with(context.adapt(ModelCamelContext::class.java).restDefinitions.find { it.path == "/my/path" }) {
            assertThat(this?.verbs).hasSize(1)

            with(this?.verbs?.get(0) as GetVerbDefinition) {
                assertThat(uri).isEqualTo("/get")
                assertThat(consumes).isEqualTo("application/json")
                assertThat(produces).isEqualTo("application/json")
                assertThat(to).hasFieldOrPropertyWithValue("endpointUri", "direct:get")
            }
        }

        with(context.adapt(ModelCamelContext::class.java).restDefinitions.find { it.path == "/post" }) {
            assertThat(this?.verbs).hasSize(1)

            with(this?.verbs?.get(0) as PostVerbDefinition) {
                assertThat(uri).isNull()
                assertThat(consumes).isEqualTo("application/json")
                assertThat(produces).isEqualTo("application/json")
                assertThat(to).hasFieldOrPropertyWithValue("endpointUri", "direct:post")
            }
        }
    }

    @Test
    fun `load integration with beans`() {
        val context = DefaultCamelContext()
        val runtime = Runtime.on(context)

        forRoutes("classpath:routes-with-beans.kts").accept(Runtime.Phase.ConfigureRoutes, runtime)

        assertThat(context.registry.findByType(DataSource::class.java)).hasSize(1)
        assertThat(context.registry.lookupByName("dataSource")).isInstanceOf(DataSource::class.java)
        assertThat(context.registry.findByType(DefaultHeaderFilterStrategy::class.java)).hasSize(1)
        assertThat(context.registry.lookupByName("filterStrategy")).isInstanceOf(DefaultHeaderFilterStrategy::class.java)
        assertThat(context.registry.lookupByName("myProcessor")).isInstanceOf(Processor::class.java)
        assertThat(context.registry.lookupByName("myPredicate")).isInstanceOf(Predicate::class.java)
    }

    @Test
    fun `load integration with components configuration`() {
        val context = DefaultCamelContext()
        val runtime = Runtime.on(context)

        forRoutes("classpath:routes-with-components-configuration.kts").accept(Runtime.Phase.ConfigureRoutes, runtime)

        val seda = context.getComponent("seda", SedaComponent::class.java)
        val mySeda = context.getComponent("mySeda", SedaComponent::class.java)
        val log = context.getComponent("log", LogComponent::class.java)

        assertThat(seda.queueSize).isEqualTo(1234)
        assertThat(seda.concurrentConsumers).isEqualTo(12)
        assertThat(mySeda.queueSize).isEqualTo(4321)
        assertThat(mySeda.concurrentConsumers).isEqualTo(21)
        assertThat(log.exchangeFormatter).isNotNull
    }

    @Test
    fun `load integration with components configuration error`() {
        val context = DefaultCamelContext()
        val runtime = Runtime.on(context)

        assertThatExceptionOfType(RuntimeCamelException::class.java)
            .isThrownBy { forRoutes("classpath:routes-with-components-configuration-error.kts").accept(Runtime.Phase.ConfigureRoutes, runtime) }
            .withCauseInstanceOf(IllegalArgumentException::class.java)
            .withMessageContaining("Type mismatch, expected: class org.apache.camel.component.log.LogComponent, got: class org.apache.camel.component.seda.SedaComponent");
    }

    @Test
    fun `load integration with languages configuration`() {
        val context = DefaultCamelContext()
        val runtime = Runtime.on(context)

        forRoutes("classpath:routes-with-languages-configuration.kts").accept(Runtime.Phase.ConfigureRoutes, runtime)

        val bean = context.resolveLanguage("bean") as BeanLanguage
        assertThat(bean.beanType).isEqualTo(String::class.java)
        assertThat(bean.method).isEqualTo("toUpperCase")

        val mybean = context.resolveLanguage("my-bean") as BeanLanguage
        assertThat(mybean.beanType).isEqualTo(String::class.java)
        assertThat(mybean.method).isEqualTo("toLowerCase")
    }

    @Test
    fun `load integration with dataformats configuration`() {
        val context = DefaultCamelContext()
        val runtime = Runtime.on(context)

        forRoutes("classpath:routes-with-dataformats-configuration.kts").accept(Runtime.Phase.ConfigureRoutes, runtime)

        val jackson = context.resolveDataFormat("json-jackson") as JacksonDataFormat
        assertThat(jackson.unmarshalType).isEqualTo(Map::class.java)
        assertThat(jackson.isPrettyPrint).isTrue()

        val myjackson = context.resolveDataFormat("my-jackson") as JacksonDataFormat
        assertThat(myjackson.unmarshalType).isEqualTo(String::class.java)
        assertThat(myjackson.isPrettyPrint).isFalse()
    }

    @Test
    fun `load integration with error handler`() {
        val context = DefaultCamelContext()
        val runtime = Runtime.on(context)

        forRoutes("classpath:routes-with-error-handler.kts").accept(Runtime.Phase.ConfigureRoutes, runtime)

        context.start()

        try {
            assertThat(context.routes).hasSize(1)
            assertThat(context.routes[0].routeContext.getOnException("my-on-exception")).isInstanceOf(FatalFallbackErrorHandler::class.java)
        } finally {
            context.stop()
        }
    }
}
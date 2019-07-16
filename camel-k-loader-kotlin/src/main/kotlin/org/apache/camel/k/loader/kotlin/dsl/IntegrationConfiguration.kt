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

import org.apache.camel.Exchange
import org.apache.camel.Predicate
import org.apache.camel.Processor
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.k.loader.kotlin.KamelKtsConfigurator
import org.apache.camel.model.*
import org.apache.camel.spi.Registry
import kotlin.script.experimental.annotations.KotlinScript

@KotlinScript(fileExtension = "kts", compilationConfiguration = KamelKtsConfigurator::class)
abstract class IntegrationConfiguration(
        private val registry : Registry,
        private val builder : RouteBuilder) : org.apache.camel.builder.BuilderSupport(builder.context) {

    fun rest(block: RestConfiguration.() -> Unit) {
        val delegate = RestConfiguration(builder)
        delegate.block()
    }

    fun context(block: ContextConfiguration.() -> Unit) {
        val delegate = ContextConfiguration(
                context = context,
                registry = registry
        )

        delegate.block()
    }

    fun processor(fn: (Exchange) -> Unit) : Processor {
        return Processor { exchange -> fn(exchange) }
    }
    fun predicate(fn: (Exchange) -> Boolean) : Predicate {
        return Predicate { exchange -> fn(exchange) }
    }

    fun from(uri: String): RouteDefinition {
        return builder.from(uri)
    }


    fun intercept() : InterceptDefinition {
        return builder.intercept()
    }

    fun onException(exception: Class<out Throwable>) : OnExceptionDefinition {
        return builder.onException(exception)
    }

    fun onCompletion() : OnCompletionDefinition {
        return builder.onCompletion()
    }

    fun interceptFrom() : InterceptFromDefinition {
        return builder.interceptFrom()
    }

    fun interceptFrom(uri: String) : InterceptFromDefinition{
        return builder.interceptFrom(uri)
    }

    fun interceptSendToEndpoint(uri: String) : InterceptSendToEndpointDefinition {
        return builder.interceptSendToEndpoint(uri)
    }
}
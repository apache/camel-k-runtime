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
package org.apache.camel.k.loader.groovy.dsl

import org.apache.camel.Exchange
import org.apache.camel.Predicate
import org.apache.camel.Processor
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.model.*
import org.apache.camel.spi.Registry

class IntegrationConfiguration extends org.apache.camel.builder.BuilderSupport {
    final Registry registry
    final Components components
    final RouteBuilder builder

    IntegrationConfiguration(RouteBuilder builder) {
        super(builder.context)

        this.registry = this.context.registry
        this.components = new Components(this.context)
        this.builder = builder
    }

    def context(@DelegatesTo(ContextConfiguration) Closure<?> callable) {
        callable.resolveStrategy = Closure.DELEGATE_FIRST
        callable.delegate = new ContextConfiguration(context)
        callable.call()
    }

    def rest(@DelegatesTo(RestConfiguration) Closure<?> callable) {
        callable.resolveStrategy = Closure.DELEGATE_FIRST
        callable.delegate = new RestConfiguration(builder)
        callable.call()
    }

    def processor(@DelegatesTo(Exchange) Closure<?> callable) {
        return new Processor() {
            @Override
            void process(Exchange exchange) throws Exception {
                callable.resolveStrategy = Closure.DELEGATE_FIRST
                callable.call(exchange)
            }
        }
    }

    def predicate(@DelegatesTo(Exchange) Closure<?> callable) {
        return new Predicate() {
            @Override
            boolean matches(Exchange exchange) throws Exception {
                callable.resolveStrategy = Closure.DELEGATE_FIRST
                return callable.call(exchange)
            }
        }
    }

    ProcessorDefinition from(String endpoint) {
        return builder.from(endpoint)
    }

    OnExceptionDefinition onException(Class<? extends Throwable> exception) {
        return builder.onException(exception)
    }

    OnCompletionDefinition onCompletion() {
        return builder.onCompletion()
    }

    InterceptDefinition intercept() {
        return builder.intercept()
    }

    InterceptFromDefinition interceptFrom() {
        return builder.interceptFrom()
    }

    InterceptFromDefinition interceptFrom(String uri) {
        return builder.interceptFrom(uri)
    }

    InterceptSendToEndpointDefinition interceptSendToEndpoint(String uri) {
        return builder.interceptSendToEndpoint(uri)
    }
}

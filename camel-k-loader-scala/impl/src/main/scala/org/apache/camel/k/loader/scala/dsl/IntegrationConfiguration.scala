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
package org.apache.camel.k.loader.scala.dsl

import org.apache.camel.builder._
import org.apache.camel.builder.endpoint._
import org.apache.camel.model._
import org.apache.camel.model.rest._

abstract class IntegrationConfiguration(private val builder: EndpointRouteBuilder)
    extends BuilderSupport(builder.getContext)
    with Support
    with EndpointBuilderFactory {

  def rest(): RestDefinition = {
    builder.rest()
  }

  def rest(block: RestConfiguration => Unit): Unit = {
    block(RestConfiguration(builder))
  }

  def beans(block: BeansConfiguration => Unit): Unit = {
    block(BeansConfiguration(getContext))
  }

  def camel(block: CamelConfiguration => Unit): Unit = {
    block(CamelConfiguration(getContext))
  }

  def from(uri: String): RouteDefinition = {
    builder.from(uri)
  }

  def from(endpoint: EndpointConsumerBuilder): RouteDefinition = {
    builder.from(endpoint)
  }

  def intercept(): InterceptDefinition = {
    builder.intercept()
  }

  def onException[T <: Throwable](exception: Class[T]): OnExceptionDefinition = {
    builder.onException(exception)
  }

  def onCompletion(): OnCompletionDefinition = {
    builder.onCompletion()
  }

  def interceptFrom(): InterceptFromDefinition = {
    builder.interceptFrom()
  }

  def interceptFrom(uri: String): InterceptFromDefinition = {
    builder.interceptFrom(uri)
  }

  def interceptSendToEndpoint(uri: String): InterceptSendToEndpointDefinition = {
    builder.interceptSendToEndpoint(uri)
  }

  def errorHandler(handler: ErrorHandlerBuilder): Unit = {
    builder.errorHandler(handler)
  }
}

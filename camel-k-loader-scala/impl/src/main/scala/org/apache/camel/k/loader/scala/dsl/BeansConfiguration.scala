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

import org.apache.camel.CamelContext
import org.apache.camel.Exchange
import org.apache.camel.Predicate
import org.apache.camel.Processor
import org.apache.camel.builder.endpoint.EndpointBuilderFactory

import scala.reflect.ClassTag

final case class BeansConfiguration(context: CamelContext) extends EndpointBuilderFactory {

  def bean[T](name: String, block: T => Unit)(implicit T: ClassTag[T]): Unit = {
    val bean = context.getInjector.newInstance(T.runtimeClass).asInstanceOf[T]
    block(bean)
    context.getRegistry.bind(name, T.runtimeClass, bean)
  }

  def bean(name: String, function: () => Any) {
    context.getRegistry.bind(name, function())
  }

  def processor(name: String, fn: Exchange => Unit) {
    context.getRegistry.bind(name, new Processor { def process(exchange: Exchange) = fn(exchange) })
  }

  def predicate(name: String, fn: Exchange => Boolean) {
    context.getRegistry.bind(name, new Predicate { def matches(exchange: Exchange) = fn(exchange) })
  }

}

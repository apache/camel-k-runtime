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
package org.apache.camel.k.loader.scala.support

import java.util

import org.apache.camel.{CamelContext, FluentProducerTemplate, RoutesBuilder}
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.k.{CompositeClassloader, Runtime}
import org.apache.camel.k.support.SourcesSupport
import org.apache.camel.model.ModelCamelContext

class TestRuntime extends Runtime {
  val context: ModelCamelContext = new DefaultCamelContext()
  context.setApplicationContextClassLoader(new CompositeClassloader())
  val template: FluentProducerTemplate = context.createFluentProducerTemplate()
  val builders: util.List[RoutesBuilder] = new util.ArrayList()
  val configurations: util.List[Any] = new util.ArrayList()

  override def getCamelContext(): CamelContext = {
    context
  }

  override def addRoutes(builder: RoutesBuilder): Unit = {
    builders.add(builder)
    context.addRoutes(builder)
  }

  def loadRoutes(routes: String*): Unit = {
    SourcesSupport.loadSources(this, routes: _*)
  }

  def start(): Unit = {
    context.start()
  }

  override def stop(): Unit = {
    context.stop()
  }

  override def close(): Unit = {
    stop()
  }
}

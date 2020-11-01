/** Licensed to the Apache Software Foundation (ASF) under one or more
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

import org.apache.camel.{CamelContext, Component}

import scala.reflect.ClassTag

final case class ComponentsConfiguration(context: CamelContext) {

  def component[T <: Component](name: String, block: T => Unit)(implicit T: ClassTag[T]): T = {
    context.getComponent(name, true, false) match {
      case null =>
        // if the component is not found, let's create a new one. This is
        // equivalent to create a new named component, useful to create
        // multiple instances of the same component but with different setup
        val target = context.getInjector.newInstance(T.runtimeClass).asInstanceOf[T]
        block(target)
        context.getRegistry.bind(name, T.runtimeClass, target)
        target
      case target: T =>
        block(target)
        target
      case target =>
        throw new IllegalArgumentException(
          s"Type mismatch, expected: ${T.runtimeClass}, got: ${target.getClass}",
        )
    }
  }

}

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

import org.apache.camel.builder.RouteBuilder
import org.apache.camel.model.rest.RestDefinition

class RestVerbConfiguration(private val builder: RouteBuilder, private val definition: RestDefinition) {

  def this(builder: RouteBuilder, path: String) = this(builder, builder.rest(path))

  def get(path: String, block: RestDefinition => Unit): Unit = block(definition.get(path))
  def get(block: RestDefinition => Unit): Unit = block(definition.get())

  def post(path: String, block: RestDefinition => Unit): Unit = block(definition.post(path))
  def post(block: RestDefinition => Unit): Unit = block(definition.post())

  def delete(path: String, block: RestDefinition => Unit): Unit = block(definition.delete(path))
  def delete(block: RestDefinition => Unit): Unit = block(definition.delete())

  def head(path: String, block: RestDefinition => Unit): Unit = block(definition.head(path))
  def head(block: RestDefinition => Unit): Unit = block(definition.head())

  def put(path: String, block: RestDefinition => Unit): Unit = block(definition.put(path))
  def put(block: RestDefinition => Unit): Unit = block(definition.put())

  def patch(path: String, block: RestDefinition => Unit): Unit = block(definition.patch(path))
  def patch(block: RestDefinition => Unit): Unit = block(definition.patch())
}

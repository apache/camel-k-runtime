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
package org.apache.camel.k.loader.kotlin.support

import org.apache.camel.k.Runtime
import org.apache.camel.CamelContext
import org.apache.camel.FluentProducerTemplate
import org.apache.camel.RoutesBuilder
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.k.CompositeClassloader
import org.apache.camel.k.support.SourcesSupport
import org.apache.camel.model.ModelCamelContext
import java.util.ArrayList

class TestRuntime : Runtime {
    val context: ModelCamelContext
    val template: FluentProducerTemplate
    val builders: MutableList<RoutesBuilder>
    val configurations: MutableList<Any>

    init {
        this.context = DefaultCamelContext()
        this.context.setApplicationContextClassLoader(CompositeClassloader())
        this.template = this.context.createFluentProducerTemplate()
        this.builders = ArrayList()
        this.configurations = ArrayList()
    }

    override fun getCamelContext(): CamelContext {
        return this.context
    }

    override fun addRoutes(builder: RoutesBuilder) {
        this.builders.add(builder)
        this.context.addRoutes(builder)
    }

    override fun addConfiguration(configuration: Any) {
        this.configurations.add(configuration)
    }

    fun loadRoutes(vararg routes: String) {
        SourcesSupport.loadSources(this, *routes)
    }

    fun start() {
        this.context.start()
    }

    override fun stop() {
        this.context.stop()
    }

    override fun close() {
        stop()
    }
}
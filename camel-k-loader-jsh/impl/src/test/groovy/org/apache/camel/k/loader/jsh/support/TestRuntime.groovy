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
package org.apache.camel.k.loader.jsh.support

import org.apache.camel.CamelContext
import org.apache.camel.RoutesBuilder
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.k.CompositeClassloader
import org.apache.camel.k.Runtime
import org.apache.camel.k.support.SourcesSupport
import org.apache.camel.model.ModelCamelContext

class TestRuntime implements Runtime, AutoCloseable {
    final ModelCamelContext context
    final List<RoutesBuilder> builders
    final List<Object> configurations

    TestRuntime() {
        this.context = new DefaultCamelContext()
        this.context.setApplicationContextClassLoader(new CompositeClassloader())
        this.builders = []
        this.configurations = []
    }

    @Override
    CamelContext getCamelContext() {
        return this.context
    }

    @Override
    void addRoutes(RoutesBuilder builder) {
        this.builders << builder
        this.context.addRoutes(builder)
    }

    void loadRoutes(String... routes) {
        SourcesSupport.loadSources(this, routes)
    }

    void start() {
        context.start()
    }

    @Override
    void stop() {
        context.stop()
    }

    @Override
    void close() {
        stop()
    }
}

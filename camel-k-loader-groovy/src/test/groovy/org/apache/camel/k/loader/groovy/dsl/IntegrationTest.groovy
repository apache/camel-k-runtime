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

import org.apache.camel.Processor
import org.apache.camel.component.log.LogComponent
import org.apache.camel.component.seda.SedaComponent
import org.apache.camel.k.Runtime
import org.apache.camel.k.listener.RoutesConfigurer
import org.apache.camel.k.main.ApplicationRuntime
import org.apache.camel.processor.FatalFallbackErrorHandler
import org.apache.camel.processor.SendProcessor
import org.apache.camel.processor.channel.DefaultChannel
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class IntegrationTest extends Specification {
    def "load integration with rest"()  {
        when:
            def runtime = new ApplicationRuntime()
            runtime.addListener(RoutesConfigurer.forRoutes('classpath:routes-with-rest.groovy'))
            runtime.addListener(Runtime.Phase.Started, { runtime.stop() })
            runtime.run()

        then:
            runtime.camelContext.restConfiguration.host == 'my-host'
            runtime.camelContext.restConfiguration.port == 9192
            runtime.camelContext.getRestConfiguration('undertow', false).host == 'my-undertow-host'
            runtime.camelContext.getRestConfiguration('undertow', false).port == 9193
            runtime.camelContext.restDefinitions.size() == 1
            runtime.camelContext.restDefinitions[0].path == '/my/path'
    }

    def "load integration with bindings"()  {
        when:
            def runtime = new ApplicationRuntime()
            runtime.addListener(RoutesConfigurer.forRoutes('classpath:routes-with-bindings.groovy'))
            runtime.addListener(Runtime.Phase.Started, { runtime.stop() })
            runtime.run()

        then:
            runtime.camelContext.registry.lookupByName('myEntry1') == 'myRegistryEntry1'
            runtime.camelContext.registry.lookupByName('myEntry2') == 'myRegistryEntry2'
            runtime.camelContext.registry.lookupByName('myEntry3') instanceof Processor
    }

    def "load integration with component configuration"()  {
        given:
            def sedaSize = new AtomicInteger()
            def sedaConsumers = new AtomicInteger()
            def mySedaSize = new AtomicInteger()
            def mySedaConsumers = new AtomicInteger()
            def format = new AtomicReference()

        when:
            def runtime = new ApplicationRuntime()
            runtime.addListener(RoutesConfigurer.forRoutes('classpath:routes-with-component-configuration.groovy'))
            runtime.addListener(Runtime.Phase.Started, {
                def seda = it.camelContext.getComponent('seda', SedaComponent)
                def mySeda = it.camelContext.getComponent('mySeda', SedaComponent)
                def log = it.camelContext.getComponent('log', LogComponent)

                assert seda != null
                assert mySeda != null
                assert log != null

                sedaSize = seda.queueSize
                sedaConsumers = seda.concurrentConsumers
                mySedaSize = mySeda.queueSize
                mySedaConsumers = mySeda.concurrentConsumers
                format = log.exchangeFormatter

                runtime.stop()
            })

            runtime.run()
        then:
            sedaSize == 1234
            sedaConsumers == 12
            mySedaSize == 4321
            mySedaConsumers == 21
            format != null
    }

    def "load integration with error handler"()  {
        given:
            def onExceptions = []
        when:
            def runtime = new ApplicationRuntime()
            runtime.addListener(RoutesConfigurer.forRoutes('classpath:routes-with-error-handler.groovy'))
            runtime.addListener(Runtime.Phase.Started, {
                it.camelContext.routes?.size() == 1
                it.camelContext.routes[0].routeContext.getOnException('my-on-exception') != null

                onExceptions << it.camelContext.routes[0].routeContext.getOnException('my-on-exception')

                runtime.stop()
            })
            runtime.run()
        then:
            onExceptions.size() == 1
            onExceptions[0] instanceof FatalFallbackErrorHandler

            def eh = onExceptions[0] as FatalFallbackErrorHandler
            def ch = eh.processor as DefaultChannel

            ch.output instanceof SendProcessor
    }
}

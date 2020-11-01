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
package org.apache.camel.k.loader.scala

import java.util

import org.apache.camel.Predicate
import org.apache.camel.Processor
import org.apache.camel.RuntimeCamelException
import org.apache.camel.component.jackson.JacksonDataFormat
import org.apache.camel.component.log.LogComponent
import org.apache.camel.component.seda.SedaComponent
import org.apache.camel.k.loader.scala.support.TestRuntime
import org.apache.camel.language.bean.BeanLanguage
import org.apache.camel.model.{FromDefinition, ProcessDefinition, ToDefinition}
import org.apache.camel.model.rest.GetVerbDefinition
import org.apache.camel.model.rest.PostVerbDefinition
import org.apache.camel.processor.FatalFallbackErrorHandler
import org.apache.camel.processor.SendProcessor
import org.apache.camel.processor.channel.DefaultChannel
import org.apache.camel.spi.HeaderFilterStrategy
import org.apache.camel.support.DefaultHeaderFilterStrategy
import spock.lang.AutoCleanup
import spock.lang.Specification
import org.scalatest.funsuite.AnyFunSuite
import org.junit.runner.RunWith
//import org.scalatestplus.junit.JUnitRunner
//import org.scalatest.junit.JUnitRunner
import org.scalatest.funsuite.AnyFunSuite
//import org.scalatest.junit.JUnitSuite
//import org.scalatest.junit.ShouldMatchersForJUnit

import scala.collection.mutable.ListBuffer
import org.junit.Test
import org.junit.Before
import org.scalatest.matchers.should.Matchers
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class ScalaSourceLoaderTest extends AnyFunSuite with Matchers {

//  test("test") {
//    assert(true)
//  }

  test("An empty Set should have size 0") {
    assert(Set.empty.size == 0)
  }

//  @Test
//  def test(): Unit = {
//    assert(Set.empty.size == 0)
//  }

  test("load routes") {
    val runtime = new TestRuntime()
    runtime.loadRoutes("classpath:routes.scala")

    val routes = runtime.context.getRouteDefinitions()
    routes should have size 1
    routes.get(0).getInput().getEndpointUri should ===("timer:tick")
    routes.get(0).getOutputs.get(0) shouldBe a[ProcessDefinition]
    routes.get(0).getOutputs.get(1) shouldBe a[ToDefinition]
  }

//    @AutoCleanup
//    def runtime = new TestRuntime()
//
//    def "load routes"() {
//        when:
//            runtime.loadRoutes("classpath:routes.groovy")
//        then:
//            with(runtime.context.routeDefinitions) {
//                it.size() == 1
//
//                it[0].outputs[0] instanceof ToDefinition
//                it[0].input.endpointUri == 'timer:tick'
//            }
//    }
//
//    def "load routes with endpoint dsl"() {
//        when:
//            runtime.loadRoutes("classpath:routes-with-endpoint-dsl.groovy")
//        then:
//            with(runtime.context.routeDefinitions) {
//                it.size() == 1
//
//                with(it[0].input, FromDefinition) {
//                    it.endpointUri == 'timer://tick?period=1s'
//                }
//                with(it[0].outputs[0], ToDefinition) {
//                    it.endpointUri == 'log://info'
//                }
//            }
//    }
//
//    def "load integration with rest"()  {
//        when:
//            runtime.loadRoutes('classpath:routes-with-rest.groovy')
//
//        then:
//            runtime.context.restConfiguration.host == 'my-host'
//            runtime.context.restConfiguration.port == 9192
//            runtime.context.restDefinitions.size() == 2
//
//            with(runtime.context.restDefinitions.find {it.path == '/my/path'}) {
//                verbs.size() == 1
//
//                with(verbs.first(), GetVerbDefinition) {
//                    uri == '/get'
//                    consumes == 'application/json'
//                    produces == 'application/json'
//
//                    with(to) {
//                        endpointUri == 'direct:get'
//                    }
//                }
//            }
//
//            with(runtime.context.restDefinitions.find {it.path == '/post'}) {
//                verbs.size() == 1
//
//                with(verbs.first(), PostVerbDefinition) {
//                    uri == null
//                    consumes == 'application/json'
//                    produces == 'application/json'
//
//                    with(to) {
//                        endpointUri == 'direct:post'
//                    }
//                }
//            }
//    }
//
//    def "load integration with beans"()  {
//        when:
//            runtime.loadRoutes('classpath:routes-with-beans.groovy')
//
//        then:
//            with(runtime.context.registry) {
//                it.findByType(MyBean).size() == 1
//                it.lookupByName('myBean') instanceof MyBean
//                it.findByType(HeaderFilterStrategy).size() == 1
//                it.lookupByName('filterStrategy') instanceof DefaultHeaderFilterStrategy
//                it.lookupByName('myProcessor') instanceof Processor
//                it.lookupByName('myPredicate') instanceof Predicate
//            }
//    }
//
//    def "load integration with components configuration"()  {
//        when:
//            runtime.loadRoutes('classpath:routes-with-components-configuration.groovy')
//
//        then:
//            with(runtime.context.getComponent('seda', SedaComponent)) {
//                queueSize == 1234
//                concurrentConsumers == 12
//            }
//            with(runtime.context.getComponent('mySeda', SedaComponent)) {
//                queueSize == 4321
//                concurrentConsumers == 21
//            }
//            with(runtime.context.getComponent('log', LogComponent)) {
//                exchangeFormatter != null
//            }
//    }
//
//    def "load integration with languages configuration"()  {
//        when:
//            runtime.loadRoutes('classpath:routes-with-languages-configuration.groovy')
//
//        then:
//            with(runtime.context.resolveLanguage('bean'), BeanLanguage) {
//                beanType == String.class
//                method == "toUpperCase"
//            }
//            with(runtime.context.resolveLanguage('myBean'), BeanLanguage) {
//                beanType == String.class
//                method == "toLowerCase"
//            }
//    }
//
//    def "load integration with dataformats configuration"()  {
//        when:
//            runtime.loadRoutes('classpath:routes-with-dataformats-configuration.groovy')
//
//        then:
//            with(runtime.context.resolveDataFormat('json-jackson'), JacksonDataFormat) {
//                unmarshalType == Map.class
//                prettyPrint
//            }
//            with(runtime.context.resolveDataFormat('my-jackson'), JacksonDataFormat) {
//                unmarshalType == String.class
//                (!prettyPrint)
//            }
//    }
//
//    def "load integration with component error property configuration"()  {
//        when:
//            runtime.loadRoutes('classpath:routes-with-component-wrong-property-configuration.groovy')
//        then:
//            def e =  thrown RuntimeCamelException
//            assert e.message.contains("No such property: queueNumber for class: org.apache.camel.component.seda.SedaComponent")
//
//    }
//
//    def "load integration with component error method configuration"()  {
//        when:
//            runtime.loadRoutes('classpath:routes-with-component-wrong-method-configuration.groovy')
//        then:
//            def e = thrown RuntimeCamelException
//            assert e.message.contains("No signature of method: org.apache.camel.component.seda.SedaComponent.queueNumber()")
//
//    }
//
//    def "load integration with error handler"()  {
//        when:
//            runtime.loadRoutes('classpath:routes-with-error-handler.groovy')
//            runtime.start()
//        then:
//            runtime.context.routes?.size() == 1
//            runtime.context.routes[0].getOnException('my-on-exception') != null
//            runtime.context.routes[0].getOnException('my-on-exception') instanceof FatalFallbackErrorHandler
//
//            def eh = runtime.context.routes[0].getOnException('my-on-exception')  as FatalFallbackErrorHandler
//            def ch = eh.processor as DefaultChannel
//
//            ch.output instanceof SendProcessor
//    }
//
//    // Test groovy eip extension, relates to https://issues.apache.org/jira/browse/CAMEL-14300
//    def "load integration with eip"()  {
//        when:
//            runtime.loadRoutes('classpath:routes-with-eip.groovy')
//            runtime.start()
//        then:
//            1 == 1
//    }
}

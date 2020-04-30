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

import org.apache.camel.Predicate
import org.apache.camel.Processor
import org.apache.camel.component.jackson.JacksonDataFormat
import org.apache.camel.component.log.LogComponent
import org.apache.camel.component.seda.SedaComponent
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.k.Runtime
import org.apache.camel.language.bean.BeanLanguage
import org.apache.camel.model.ModelCamelContext
import org.apache.camel.model.rest.GetVerbDefinition
import org.apache.camel.model.rest.PostVerbDefinition
import org.apache.camel.processor.FatalFallbackErrorHandler
import org.apache.camel.processor.SendProcessor
import org.apache.camel.processor.channel.DefaultChannel
import org.apache.camel.spi.HeaderFilterStrategy
import org.apache.camel.support.DefaultHeaderFilterStrategy
import spock.lang.Specification

import javax.sql.DataSource

import static org.apache.camel.k.listener.RoutesConfigurer.forRoutes

class IntegrationTest extends Specification {

    private ModelCamelContext context
    private Runtime runtime

    def setup() {
        this.context = new DefaultCamelContext()
        this.runtime = Runtime.on(context)
    }


    def cleanup() {
        if (this.context != null) {
            this.context.stop()
        }
    }

    def "load integration with rest"()  {
        when:
            forRoutes('classpath:routes-with-rest.groovy').accept(Runtime.Phase.ConfigureRoutes, runtime)

        then:
            context.restConfiguration.host == 'my-host'
            context.restConfiguration.port == 9192

            context.restDefinitions.size() == 2

            with(context.restDefinitions.find {it.path == '/my/path'}) {
                verbs.size() == 1

                with(verbs.first(), GetVerbDefinition) {
                    uri == '/get'
                    consumes == 'application/json'
                    produces == 'application/json'

                    with(to) {
                        endpointUri == 'direct:get'
                    }
                }
            }

            with(context.restDefinitions.find {it.path == '/post'}) {
                verbs.size() == 1

                with(verbs.first(), PostVerbDefinition) {
                    uri == null
                    consumes == 'application/json'
                    produces == 'application/json'

                    with(to) {
                        endpointUri == 'direct:post'
                    }
                }
            }
    }

    def "load integration with beans"()  {
        when:
            forRoutes('classpath:routes-with-beans.groovy').accept(Runtime.Phase.ConfigureRoutes, runtime)

        then:
            context.registry.findByType(DataSource).size() == 1
            context.registry.lookupByName('dataSource') instanceof DataSource
            context.registry.findByType(HeaderFilterStrategy).size() == 1
            context.registry.lookupByName('filterStrategy') instanceof DefaultHeaderFilterStrategy

            context.registry.lookupByName('myProcessor') instanceof Processor
            context.registry.lookupByName('myPredicate') instanceof Predicate
    }

    def "load integration with components configuration"()  {
        when:
            forRoutes('classpath:routes-with-components-configuration.groovy').accept(Runtime.Phase.ConfigureRoutes, runtime)

        then:
            with(context.getComponent('seda', SedaComponent)) {
                queueSize == 1234
                concurrentConsumers == 12
            }
            with(context.getComponent('mySeda', SedaComponent)) {
                queueSize == 4321
                concurrentConsumers == 21
            }
            with(context.getComponent('log', LogComponent)) {
                exchangeFormatter != null
            }
    }

    def "load integration with languages configuration"()  {
        when:
            forRoutes('classpath:routes-with-languages-configuration.groovy').accept(Runtime.Phase.ConfigureRoutes, runtime)

        then:
            with(context.resolveLanguage('bean'), BeanLanguage) {
                beanType == String.class
                method == "toUpperCase"
            }
            with(context.resolveLanguage('myBean'), BeanLanguage) {
                beanType == String.class
                method == "toLowerCase"
            }
    }

    def "load integration with dataformats configuration"()  {
        when:
            forRoutes('classpath:routes-with-dataformats-configuration.groovy').accept(Runtime.Phase.ConfigureRoutes, runtime)

        then:
            with(context.resolveDataFormat('json-jackson'), JacksonDataFormat) {
                unmarshalType == Map.class
                prettyPrint == true
            }
            with(context.resolveDataFormat('my-jackson'), JacksonDataFormat) {
                unmarshalType == String.class
                prettyPrint == false
            }
    }

    def "load integration with component error property configuration"()  {
        when:
            forRoutes('classpath:routes-with-component-wrong-property-configuration.groovy').accept(Runtime.Phase.ConfigureRoutes, runtime)
        then:
            def e =  thrown org.apache.camel.RuntimeCamelException
            assert e.message.contains("No such property: queueNumber for class: org.apache.camel.component.seda.SedaComponent")

    }

    def "load integration with component error method configuration"()  {
        when:
            forRoutes('classpath:routes-with-component-wrong-method-configuration.groovy').accept(Runtime.Phase.ConfigureRoutes, runtime)
        then:
            def e =  thrown org.apache.camel.RuntimeCamelException
            assert e.message.contains("No signature of method: org.apache.camel.component.seda.SedaComponent.queueNumber()")

    }

    def "load integration with error handler"()  {
        when:
            forRoutes('classpath:routes-with-error-handler.groovy').accept(Runtime.Phase.ConfigureRoutes, runtime)

            context.start()
        then:
            context.routes?.size() == 1
            context.routes[0].routeContext.getOnException('my-on-exception') != null
            context.routes[0].routeContext.getOnException('my-on-exception') instanceof FatalFallbackErrorHandler

            def eh = context.routes[0].routeContext.getOnException('my-on-exception')  as FatalFallbackErrorHandler
            def ch = eh.processor as DefaultChannel

            ch.output instanceof SendProcessor
    }

    // Test groovy eip extension, relates to https://issues.apache.org/jira/browse/CAMEL-14300
    def "load integration with eip"()  {
        when:
            forRoutes('classpath:routes-with-eip.groovy').accept(Runtime.Phase.ConfigureRoutes, runtime)

            context.start()
        then:
            1 == 1
    }
}


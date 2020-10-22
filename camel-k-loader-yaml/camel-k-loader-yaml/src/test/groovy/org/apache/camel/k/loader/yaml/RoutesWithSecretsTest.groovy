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
package org.apache.camel.k.loader.yaml


import org.apache.camel.component.telegram.TelegramEndpoint
import org.apache.camel.k.loader.yaml.support.TestSupport

class RoutesWithSecretsTest extends TestSupport {
    static final Properties PROPERTIES = [
        'telegram.token': 's3cret',
    ]

    def 'route'() {
        setup:
            def context = startContextForSpec {
                propertiesComponent.initialProperties = PROPERTIES
            }
        when:
            def eps = context.getEndpoints().findAll { it instanceof TelegramEndpoint }
        then:
            eps.each {
                with(it, TelegramEndpoint) {
                    endpointUri == 'telegram://bots?authorizationToken=RAW(s3cret)'
                    configuration.authorizationToken == PROPERTIES.getProperty('telegram.token')
                }
            }
        cleanup:
            context?.stop()
    }

    def 'route_property'() {
        setup:
            def context = startContextForSpec {
                propertiesComponent.initialProperties = PROPERTIES
            }
        when:
            def eps = context.getEndpoints().findAll { it instanceof TelegramEndpoint }
        then:
            eps.each {
                with(it, TelegramEndpoint) {
                    endpointUri == 'telegram://bots?authorizationToken=%23property:telegram.token'
                    configuration.authorizationToken == PROPERTIES.getProperty('telegram.token')
                }
            }
        cleanup:
            context?.stop()
    }

    def 'route_raw'() {
        setup:
            def context = startContextForSpec {
                propertiesComponent.initialProperties = PROPERTIES
            }
        when:
            def eps = context.getEndpoints().findAll { it instanceof TelegramEndpoint }
        then:
            eps.each {
                with(it, TelegramEndpoint) {
                    endpointUri == 'telegram://bots?authorizationToken=RAW(s3cret)'
                    configuration.authorizationToken == PROPERTIES.getProperty('telegram.token')
                }
            }
        cleanup:
            context?.stop()
    }
}

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

import org.apache.camel.component.direct.DirectEndpoint

class RoutesWithPlaceholdersTest extends TestSupport {
    def 'route'() {
        setup:
            def parameters = [
                'direct.id': 'myDirect',
                'direct.timeout': 1234,
                'direct.result': UUID.randomUUID().toString()
            ]
            def context = startContextForSpec {
                propertiesComponent.initialProperties = parameters as Properties
            }
        when:
            def uri = context.resolvePropertyPlaceholders('direct://{{direct.id}}?timeout={{direct.timeout}}')
            def out = template(context).to(uri).request(String.class)
        then:
            out == parameters['direct.result']
        cleanup:
            context?.stop()
    }
    def 'from'() {
        setup:
            def parameters = [
                'direct.id': 'myDirect',
                'direct.timeout': 1234,
                'direct.result': UUID.randomUUID().toString()
            ]
            def context = startContextForSpec {
                propertiesComponent.initialProperties = parameters as Properties
            }
        when:
            def uri = context.resolvePropertyPlaceholders('direct://{{direct.id}}?timeout={{direct.timeout}}')
            def out = template(context).to(uri).request(String.class)
            def eps = context.getEndpoints().find { it instanceof DirectEndpoint }
        then:
            out == parameters['direct.result']
            with (eps, DirectEndpoint) {
                timeout == parameters['direct.timeout']
            }
        cleanup:
            context?.stop()
    }

    def 'to'() {
        setup:
            def parameters = [
                'direct.id': 'myDirect',
                'direct.timeout': 1234,
                'direct.result': UUID.randomUUID().toString()
            ]
            def context = startContextForSpec {
                propertiesComponent.initialProperties = parameters as Properties
            }
        when:
            def out = template(context).to('direct:start').request(String.class)
        then:
            out == parameters['direct.result']
        cleanup:
            context?.stop()
    }

    def 'tod'() {
        setup:
            def parameters = [
                'direct.id': 'myDirect',
                'direct.timeout': 1234,
                'direct.result': UUID.randomUUID().toString()
            ]
            def context = startContextForSpec {
                propertiesComponent.initialProperties = parameters as Properties
            }
        when:
            def out = template(context).to('direct:start').request(String.class)
        then:
            out == parameters['direct.result']
        cleanup:
            context?.stop()
    }
}

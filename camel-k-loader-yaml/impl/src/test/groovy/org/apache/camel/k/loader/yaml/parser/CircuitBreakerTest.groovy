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
package org.apache.camel.k.loader.yaml.parser

import org.apache.camel.k.loader.yaml.support.TestSupport
import org.apache.camel.model.CircuitBreakerDefinition
import org.apache.camel.model.LogDefinition

class CircuitBreakerTest extends TestSupport {
    def "definition"() {
        when:
            def definition = toDefinition('circuit-breaker\'', '''
                 configuration-ref: "my-config"
                 resilience4j-configuration:
                   failure-rate-threshold: "10"
                 hystrix-configuration:
                   group-key: "my-group"
                 on-fallback:
                   fallback-via-network: "true"
                 steps:
                   - log:
                       message: "test"
            ''')
        then:
            with(definition, CircuitBreakerDefinition) {
                configurationRef == 'my-config'

                resilience4jConfiguration != null
                resilience4jConfiguration.failureRateThreshold == '10'

                hystrixConfiguration != null
                hystrixConfiguration.groupKey == 'my-group'

                onFallback != null
                onFallback.fallbackViaNetwork == "true"
            }
    }

    def "definition with steps"() {
        when:
            def definition = toDefinition('circuit-breaker', '''
                 steps:
                   - log: "info"
                 on-fallback:
                     steps:
                       - log: "fallback"
            ''')
        then:
            with(definition, CircuitBreakerDefinition) {
                with (outputs[0], LogDefinition) {
                    message == "info"
                }
                with (onFallback.outputs[0], LogDefinition) {
                    message == "fallback"
                }
            }
    }
}


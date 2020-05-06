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


import org.apache.camel.model.RemovePropertiesDefinition

class RemovePropertiesTest extends org.apache.camel.k.loader.yaml.TestSupport {

    def "definition"() {
        given:
            def stepContext = stepContext('''
                 pattern: toRemove
                 exclude-pattern: toExclude
                 exclude-patterns:
                   - toExclude1
                   - toExclude2
            ''')
        when:
            def processor = toProcessor('remove-properties', '''
                 pattern: toRemove
                 exclude-pattern: toExclude
                 exclude-patterns:
                   - toExclude1
                   - toExclude2
            ''')
        then:
            with(processor, RemovePropertiesDefinition) {
                pattern == 'toRemove'
                excludePattern == 'toExclude'

                excludePatterns.contains('toExclude1')
                excludePatterns.contains('toExclude2')
            }
    }

}

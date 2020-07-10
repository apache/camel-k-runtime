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

import org.apache.camel.builder.DeadLetterChannelBuilder
import org.apache.camel.builder.DefaultErrorHandlerBuilder
import org.apache.camel.builder.ErrorHandlerBuilderRef
import org.apache.camel.builder.NoErrorHandlerBuilder
import org.apache.camel.k.loader.yaml.TestSupport
import org.apache.camel.model.RouteDefinition

class ErrorHandlerTest extends TestSupport {
    def "definition (route/no-error-handler)"() {
        when:
            def processor = toProcessor(ErrorHandlerStepParser, '''
                 no-error-handler: {}                 
            ''')
        then:
            with(processor, RouteDefinition) {
                errorHandlerFactory instanceof NoErrorHandlerBuilder
            }
    }

    def "definition (route/default)"() {
        when:
            def processor = toProcessor(ErrorHandlerStepParser, '''
                 default:
                   dead-letter-uri: "jms:queue:dead"   
            ''')
        then:
            with(processor, RouteDefinition) {
                with(errorHandlerFactory, DefaultErrorHandlerBuilder) {
                    deadLetterUri == 'jms:queue:dead'
                }
            }
    }

    def "definition (route/dead-letter-channel)"() {
        when:
            def processor = toProcessor(ErrorHandlerStepParser, '''
                 dead-letter-channel: "jms:queue:dead"  
            ''')
        then:
            with(processor, RouteDefinition) {
                with(errorHandlerFactory, DeadLetterChannelBuilder) {
                    deadLetterUri == 'jms:queue:dead'
                }
            }
    }

    def "definition (route/ref)"() {
        when:
            def processor = toProcessor(ErrorHandlerStepParser, '''
                 ref: "myErrorHandler"                 
            ''')
        then:
            with(processor, RouteDefinition) {
                with(errorHandlerFactory, ErrorHandlerBuilderRef) {
                    ref == 'myErrorHandler'
                }
            }
    }

    def "definition (global/no-error-handler)"() {
        given:
            def stepContext = stepContext('''
                 no-error-handler: {}                 
            ''')
        when:
            new ErrorHandlerStepParser().process(stepContext)
        then:
            stepContext.builder().routeCollection.errorHandlerFactory instanceof NoErrorHandlerBuilder
    }

    def "definition (global/default)"() {
        given:
            def stepContext = stepContext('''
                 default:
                   dead-letter-uri: "jms:queue:dead"        
            ''')
        when:
            new ErrorHandlerStepParser().process(stepContext)
        then:
            with(stepContext.builder().routeCollection.errorHandlerFactory, DefaultErrorHandlerBuilder) {
                deadLetterUri == 'jms:queue:dead'
            }
    }

    def "definition (global/dead-letter-channel)"() {
        given:
            def stepContext = stepContext('''
                 dead-letter-channel: "jms:queue:dead"        
            ''')
        when:
            new ErrorHandlerStepParser().process(stepContext)
        then:
            with(stepContext.builder().routeCollection.errorHandlerFactory, DefaultErrorHandlerBuilder) {
                deadLetterUri == 'jms:queue:dead'
            }
    }

    def "definition (global/ref)"() {
        given:
            def stepContext = stepContext('''
                 ref: "myErrorHandler"          
            ''')
        when:
            new ErrorHandlerStepParser().process(stepContext)
        then:
            with(stepContext.builder().routeCollection.errorHandlerFactory, ErrorHandlerBuilderRef) {
                ref == 'myErrorHandler'
            }
    }
}

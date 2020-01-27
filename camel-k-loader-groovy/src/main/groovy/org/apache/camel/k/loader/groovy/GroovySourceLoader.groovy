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
package org.apache.camel.k.loader.groovy


import org.apache.camel.builder.endpoint.EndpointRouteBuilder
import org.apache.camel.k.Runtime
import org.apache.camel.k.Source
import org.apache.camel.k.SourceLoader
import org.apache.camel.k.loader.groovy.dsl.IntegrationConfiguration
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer

class GroovySourceLoader implements SourceLoader {
    @Override
    List<String> getSupportedLanguages() {
        return Collections.singletonList('groovy')
    }

    @Override
    Result load(Runtime runtime, Source source) throws Exception {
        def builder = new EndpointRouteBuilder() {
            @Override
            void configure() throws Exception {
                def ic = new ImportCustomizer()
                ic.addStarImports('org.apache.camel')
                ic.addStarImports('org.apache.camel.spi')

                def cc = new CompilerConfiguration()
                cc.addCompilationCustomizers(ic)
                cc.setScriptBaseClass(DelegatingScript.class.getName())

                def cl = Thread.currentThread().getContextClassLoader()
                def sh = new GroovyShell(cl, new Binding(), cc)
                def is = source.resolveAsInputStream(getContext())

                is.withCloseable {
                    def reader = new InputStreamReader(is)
                    def script = (DelegatingScript) sh.parse(reader)

                    // set the delegate target
                    script.setDelegate(new IntegrationConfiguration(this))
                    script.run()
                }
            }
        }

        return Result.on(builder)
    }
}

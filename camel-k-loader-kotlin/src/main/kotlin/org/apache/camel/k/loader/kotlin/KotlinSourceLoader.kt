/**
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
package org.apache.camel.k.loader.kotlin

import org.apache.camel.RuntimeCamelException
import org.apache.camel.builder.endpoint.EndpointRouteBuilder
import org.apache.camel.k.Runtime
import org.apache.camel.k.Source
import org.apache.camel.k.SourceLoader
import org.apache.camel.k.loader.kotlin.dsl.IntegrationConfiguration
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.io.InputStreamReader
import kotlin.script.experimental.api.ResultValue
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.constructorArgs
import kotlin.script.experimental.api.valueOrNull
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.BasicJvmScriptEvaluator
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.JvmScriptCompiler
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate

class KotlinSourceLoader : SourceLoader {
    companion object {
        private val LOGGER : Logger = LoggerFactory.getLogger(KotlinSourceLoader::class.java)

        @JvmStatic
        fun load(inputStream: InputStream): EndpointRouteBuilder {
            return object : EndpointRouteBuilder() {
                @Throws(Exception::class)
                override fun configure() {
                    load(inputStream, this)
                }
            }
        }

        @JvmStatic
        fun load(inputStream: InputStream, builder: EndpointRouteBuilder) {
            val compiler = JvmScriptCompiler()
            val evaluator = BasicJvmScriptEvaluator()
            val host = BasicJvmScriptingHost(compiler = compiler, evaluator = evaluator)
            val config = createJvmCompilationConfigurationFromTemplate<IntegrationConfiguration>()

            val result = host.eval(
                    InputStreamReader(inputStream).readText().toScriptSource(),
                    config,
                    ScriptEvaluationConfiguration {
                        //
                        // Arguments used to initialize the script base class
                        //
                        constructorArgs(builder)
                    }
            )

            // ensure that evaluation errors propagation
            when(val rv = result.valueOrNull()?.returnValue) {
                is ResultValue.Error -> throw RuntimeCamelException(rv.error)
            }

            for (report in result.reports) {
                when (report.severity) {
                    ScriptDiagnostic.Severity.FATAL -> LOGGER.error("{}", report.message, report.exception)
                    ScriptDiagnostic.Severity.ERROR -> LOGGER.error("{}", report.message, report.exception)
                    ScriptDiagnostic.Severity.WARNING -> LOGGER.warn("{}", report.message, report.exception)
                    ScriptDiagnostic.Severity.INFO -> LOGGER.info("{}", report.message)
                    ScriptDiagnostic.Severity.DEBUG -> LOGGER.debug("{}", report.message)
                }
            }
        }
    }

    override fun getSupportedLanguages(): List<String> {
        return listOf("kts")
    }

    @Throws(Exception::class)
    override fun load(runtime: Runtime, source: Source): SourceLoader.Result {
        return SourceLoader.Result.on(object : EndpointRouteBuilder() {
            @Throws(Exception::class)
            override fun configure() {
                source.resolveAsInputStream(runtime.camelContext).use {
                    load(it,  this)
                }
            }
        })
    }
}

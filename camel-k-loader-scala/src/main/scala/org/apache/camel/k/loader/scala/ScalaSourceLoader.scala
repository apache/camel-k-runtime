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
package org.apache.camel.k.loader.scala

import java.io.{BufferedReader, Reader}
import java.util

import org.apache.camel.RoutesBuilder
import org.apache.camel.builder.endpoint.EndpointRouteBuilder
import org.apache.camel.k.loader.scala.ScalaSourceLoader._
import org.apache.camel.k.loader.scala.dsl.IntegrationConfiguration
import org.apache.camel.k.support.RouteBuilders
import org.apache.camel.k.{Runtime, Source, SourceLoader}
import org.slf4j.LoggerFactory

import scala.reflect.runtime.universe.runtimeMirror
import scala.tools.reflect.ToolBox
import scala.util.control.NonFatal

class ScalaSourceLoader extends SourceLoader {

  /**
   * Provides a list of the languages supported by this loader.
   *
   * @return the supported languages.
   */
  override def getSupportedLanguages: util.List[String] = util.List.of("scala")

  /**
   * Creates a camel `RoutesBuilder` from the given resource.
   *
   * @param runtime the runtime.
   * @param source  the source to load.
   * @return the RoutesBuilder.
   */
  override def load(runtime: Runtime, source: Source): RoutesBuilder =
    RouteBuilders.endpoint(source, { (reader, builder) => doLoad(reader, builder) })

  private def doLoad(reader: Reader, builder: EndpointRouteBuilder): Unit = {

    val result = for {
      source <- loadSource(reader)
      executable <- compile[Any](source)
      result <- execute(builder)(executable)
    } yield result

    val logger = LoggerFactory.getLogger(classOf[ScalaSourceLoader])
    result match {
      case Left(ScalaError.SourceLoading(e)) =>
        logger.error("Error while loading Scala source", e)
      case Left(ScalaError.Compilation(e)) =>
        logger.error("Error while compiling Scala code", e)
      case Left(ScalaError.Execution(e)) =>
        logger.error("Error while executing Scala code", e)
      case Right(_) => ()
    }
  }

  private def loadSource(reader: Reader): Either[ScalaError.SourceLoading, String] = try {
    val bufferedReader = new BufferedReader(reader)
    val result = LazyList
      .continually(bufferedReader.readLine())
      .takeWhile(_ != null)
      .mkString("\n")
    Right(result)
  } catch {
    case NonFatal(e) => Left(ScalaError.SourceLoading(e))
  }

  private def compile[A](source: String): Either[ScalaError.Compilation, IntegrationConfiguration => A] =
    try {
      val tb = runtimeMirror(classOf[IntegrationConfiguration].getClassLoader).mkToolBox()
      val tree = tb.parse(s"""
        |def wrapper(integrationConfiguration: org.apache.camel.k.loader.scala.dsl.IntegrationConfiguration): Any = {
        |  import integrationConfiguration._
        |  $source
        |}
        |wrapper _
      """.stripMargin)
      val f = tb.compile(tree)
      val wrapper = f()
      val result = wrapper.asInstanceOf[IntegrationConfiguration => A]
      Right(result)
    } catch {
      case NonFatal(e) => Left(ScalaError.Compilation(e))
    }

  private def execute[A](
      builder: EndpointRouteBuilder,
  )(executable: IntegrationConfiguration => A): Either[ScalaError.Execution, A] = try {
    val result = executable(new IntegrationConfiguration(builder) {})
    Right(result)
  } catch {
    case NonFatal(e) => Left(ScalaError.Execution(e))
  }

}

object ScalaSourceLoader {
  sealed abstract class ScalaError(cause: Throwable) extends Exception(cause) with Product with Serializable
  object ScalaError {
    case class SourceLoading(cause: Throwable) extends ScalaError(cause)
    case class Compilation(cause: Throwable) extends ScalaError(cause)
    case class Execution(cause: Throwable) extends ScalaError(cause)
  }
}

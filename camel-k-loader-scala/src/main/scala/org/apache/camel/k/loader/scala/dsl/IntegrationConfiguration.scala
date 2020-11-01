package org.apache.camel.k.loader.scala.dsl

import org.apache.camel.builder._
import org.apache.camel.builder.endpoint._
import org.apache.camel.model._
import org.apache.camel.model.rest._

abstract class IntegrationConfiguration(private val builder: EndpointRouteBuilder)
    extends BuilderSupport(builder.getContext)
    with Support
    with EndpointBuilderFactory {

  def rest(): RestDefinition = {
    builder.rest()
  }

  def rest(block: RestConfiguration => Unit): Unit = {
    block(RestConfiguration(builder))
  }

  def beans(block: BeansConfiguration => Unit): Unit = {
    block(BeansConfiguration(getContext))
  }

  def camel(block: CamelConfiguration => Unit): Unit = {
    block(CamelConfiguration(getContext))
  }

  def from(uri: String): RouteDefinition = {
    builder.from(uri)
  }

  def from(endpoint: EndpointConsumerBuilder): RouteDefinition = {
    builder.from(endpoint)
  }

  def intercept(): InterceptDefinition = {
    builder.intercept()
  }

  def onException[T <: Throwable](exception: Class[T]): OnExceptionDefinition = {
    builder.onException(exception)
  }

  def onCompletion(): OnCompletionDefinition = {
    builder.onCompletion()
  }

  def interceptFrom(): InterceptFromDefinition = {
    builder.interceptFrom()
  }

  def interceptFrom(uri: String): InterceptFromDefinition = {
    builder.interceptFrom(uri)
  }

  def interceptSendToEndpoint(uri: String): InterceptSendToEndpointDefinition = {
    builder.interceptSendToEndpoint(uri)
  }

  def errorHandler(handler: ErrorHandlerBuilder): Unit = {
    builder.errorHandler(handler)
  }
}

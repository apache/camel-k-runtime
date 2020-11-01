package org.apache.camel.k.loader.scala.dsl

import org.apache.camel.{Exchange, Predicate, Processor}

trait Support {

  def processor(fn: Exchange => Unit): Processor = {
    { exchange => fn(exchange) }
  }

  def predicate(fn: Exchange => Boolean): Predicate = {
    { exchange => fn(exchange) }
  }

}

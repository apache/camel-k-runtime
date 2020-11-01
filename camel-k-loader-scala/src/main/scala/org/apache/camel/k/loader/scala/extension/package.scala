package org.apache.camel.k.loader.scala

import org.apache.camel.Exchange
import org.apache.camel.component.log.LogComponent

package object `extension` {
  implicit class LogComponentExtension(private val log: LogComponent) extends AnyVal {
    def formatter(fmt: Exchange => String) {
      log.setExchangeFormatter((exchange: Exchange) => fmt(exchange))
    }
  }
}

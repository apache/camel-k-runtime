package org.apache.camel.k.loader.scala.`extension`

import org.apache.camel.component.log.LogComponent
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.support.DefaultExchange
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class LogExtensionTest extends AnyFunSuite with Matchers {
  test("invoke extension method - formatter") {
    val ctx = new DefaultCamelContext()

    var log = new LogComponent()
    log.formatter { e =>
      "body: " + e.getIn().getBody()
    }

    var ex = new DefaultExchange(ctx)
    ex.getIn().setBody("hello")

    log.getExchangeFormatter.format(ex) should ===("body: hello")
  }
}

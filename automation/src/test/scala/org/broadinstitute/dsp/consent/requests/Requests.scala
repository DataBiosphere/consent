package org.broadinstitute.dsp.consent.requests

import java.net.URLEncoder

import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._
import org.broadinstitute.dsp.consent.TestConfig

object Requests {

  val rootRequest: ChainBuilder = exec(
    http("Root URL")
      .get("/")
      .check(status.is(session => 200))
  )

  val statusRequest: ChainBuilder = exec(
    http("Status Request")
      .get("/status")
      .headers(TestConfig.jsonHeader)
      .check(status.is(session => 200))
  )

  val versionRequest: ChainBuilder = exec(
    http("Version Request")
      .get("/version")
      .headers(TestConfig.jsonHeader)
      .check(status.is(session => 200))
  )

  private def encode(term: String): String = {
    URLEncoder.encode(term, "UTF-8")
  }

  object Dac {
    val list: ChainBuilder = exec(
      http("Dac List")
        .get("/api/dac")
        .headers(TestConfig.jsonHeader)
        .headers(TestConfig.adminHeader)
        .check(status.is(session => 200))
    )

  }

}

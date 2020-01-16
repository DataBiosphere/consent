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
    val dacListResponse: String = "DAC_LIST_RESPONSE"
    def list(expectedStatus: Int, additionalHeaders: Map[String, String]): ChainBuilder = exec(
      http("Dac List")
        .get("/api/dac")
        .headers(TestConfig.jsonHeader)
        .headers(additionalHeaders)
        .check(bodyString.saveAs(dacListResponse))
        .check(status.is(expectedStatus))
    )

    def listResults(expectedStatus: Int, additionalHeaders: Map[String, String]): String = {
      var responseContent: String = ""
      list(expectedStatus, additionalHeaders).exec(
        session => {
          responseContent = session(dacListResponse).as[String]
          session
        }
      )
      responseContent
    }

  }

}

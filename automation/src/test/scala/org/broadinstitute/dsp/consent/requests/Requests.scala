package org.broadinstitute.dsp.consent.requests

import java.net.URLEncoder

import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.request.builder._
import io.gatling.http.Predef._
import org.broadinstitute.dsp.consent.TestConfig
import org.broadinstitute.dsp.consent.models._
import spray.json._

object Requests {

  val rootRequest: HttpRequestBuilder = {
    http("Root URL")
      .get("/")
      .check(status.is(session => 200))
  }

  val statusRequest: HttpRequestBuilder = {
    http("Status Request")
      .get("/status")
      .headers(TestConfig.jsonHeader)
      .check(status.is(session => 200))
  }

  val versionRequest: HttpRequestBuilder = {
    http("Version Request")
      .get("/version")
      .headers(TestConfig.jsonHeader)
      .check(status.is(session => 200))
  }

  private def encode(term: String): String = {
    URLEncoder.encode(term, "UTF-8")
  }

  object Dac {
    val dacListResponse: String = "DAC_LIST_RESPONSE"
    def list(expectedStatus: Int, additionalHeaders: Map[String, String]): HttpRequestBuilder = {
      http("Dac List")
        .get("/api/dac")
        .headers(TestConfig.jsonHeader)
        .headers(additionalHeaders)
        .check(bodyString.saveAs(dacListResponse))
        .check(status.is(expectedStatus))
    }

    def listResults(expectedStatus: Int, additionalHeaders: Map[String, String]): String = {
      var responseContent: String = ""
      exec(list(expectedStatus, additionalHeaders)).exec(
        session => {
          responseContent = session(dacListResponse).as[String]
          session
        }
      )
      responseContent
    }

  }

  object User {
    val userResponse: String = "USER_RESPONSE"
    val dacUserId: String = "dacUserId"
    def me(expectedStatus: Int, additionalHeaders: Map[String, String]): HttpRequestBuilder = {
      http("Get User")
        .get("/api/user/me")
        .headers(TestConfig.jsonHeader)
        .headers(additionalHeaders)
        .check(bodyString.saveAs(userResponse))
        .check(jsonPath("$.dacUserId").saveAs(dacUserId))
        .check(status.is(expectedStatus))
    }

    def userResult(expectedStatus: Int, additionalHeaders: Map[String, String]): String = {
      var responseContent: String = ""
      exec(me(expectedStatus, additionalHeaders)).exec(
        session => {
          responseContent = session(userResponse).as[String]
          println(responseContent)
          session
        }
      )
      responseContent
    }

    def dataSetCatalog(expectedStatus: Int, dacUserId: String, additionalHeaders: Map[String, String]): HttpRequestBuilder = {
      http("DataSet Catalog Requests")
        .get("/")
        .headers(TestConfig.jsonHeader)
        .headers(additionalHeaders)
        .resources(
          DataSet.byUserId(expectedStatus, dacUserId, additionalHeaders),
          Dac.list(expectedStatus, additionalHeaders)
        )
    }
  }

  object DataSet {
    val dataSetResponse: String = "DATASET_RESPONSE"
    def byUserId(expectedStatus: Int, dacUserId: String, additionalHeaders: Map[String, String]): HttpRequestBuilder = {
      http("Get DataSet By User")
            .get("/api/dataset?dacUserId=" + dacUserId)
            .headers(TestConfig.jsonHeader)
            .headers(additionalHeaders)
            .check(bodyString.saveAs(dataSetResponse))
            .check(status.is(expectedStatus))
    }
  }

  object Dar {
    val darPartialResponse: String = "DAR_RESPONSE"
    val darRequestBody: String = "darRequestBody"
    val darReferenceId: String = "darReferenceId"
    val darId: String = "darId"
    def partialSave(expectedStatus: Int, body: String, additionalHeaders: Map[String, String]): HttpRequestBuilder = {
        http("Save Partial Dar")
          .post("/api/dar/v2/draft")
          .headers(TestConfig.jsonHeader)
          .headers(additionalHeaders)
          .body(StringBody(body)).asJson
          .check(jsonPath("$.referenceId").saveAs(darReferenceId))
          .check(jsonPath("$.id").saveAs(darId))
          .check(bodyString.saveAs(darPartialResponse))
          .check(status.is(expectedStatus))
    }
  }
}

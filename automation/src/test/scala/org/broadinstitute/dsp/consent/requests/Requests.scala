package org.broadinstitute.dsp.consent.requests

import java.net.URLEncoder

import io.gatling.core.Predef._
import io.gatling.http.request.builder._
import io.gatling.http.Predef._
import org.broadinstitute.dsp.consent.TestConfig
import io.netty.handler.codec.http.HttpResponseStatus._

object Requests {

  val rootRequest: HttpRequestBuilder = {
    http("Root URL")
      .get("/")
      .check(status.is(session => OK.code))
  }

  val statusRequest: HttpRequestBuilder = {
    http("Status Request")
      .get("/status")
      .headers(TestConfig.jsonHeader)
      .check(status.is(session => OK.code))
  }

  val versionRequest: HttpRequestBuilder = {
    http("Version Request")
      .get("/version")
      .headers(TestConfig.jsonHeader)
      .check(status.is(session => OK.code))
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
    val userResponse: String = "userResponse"
    val dacUserId: String = "dacUserId"
    val userByIdResponse: String = "userByIdResponse"

    def me(expectedStatus: Int, additionalHeaders: Map[String, String]): HttpRequestBuilder = {
      http("Get User")
        .get("/api/user/me")
        .headers(TestConfig.jsonHeader)
        .headers(additionalHeaders)
        .check(bodyString.saveAs(userResponse))
        .check(jsonPath("$.dacUserId").saveAs(dacUserId))
        .check(status.is(expectedStatus))
    }

    def getById(expectedStatus: Int, userId: String, additionalHeaders: Map[String, String]): HttpRequestBuilder = {
      http("Get User By Id")
        .get(s"api/user/$userId")
        .headers(TestConfig.jsonHeader)
        .headers(additionalHeaders)
        .check(bodyString.saveAs(userByIdResponse))
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
    val dataSetsByDataSetId: String = "DATASET_BYDSID_"

    def byUserId(expectedStatus: Int, dacUserId: String, additionalHeaders: Map[String, String]): HttpRequestBuilder = {
      http("Get DataSet By User")
        .get("/api/dataset?dacUserId=" + dacUserId)
        .headers(TestConfig.jsonHeader)
        .headers(additionalHeaders)
        .check(bodyString.saveAs(dataSetResponse))
        .check(status.is(expectedStatus))
    }

    def getDataSetsByDataSetId(expectedStatus: Int, dataSetId: String, additionalHeaders: Map[String, String], index: Int = 0): HttpRequestBuilder = {
      http("Get DataSets By Data Set ID")
        .get("/api/dataset/" + dataSetId)
        .headers(TestConfig.jsonHeader)
        .headers(additionalHeaders)
        .check(bodyString.saveAs(dataSetsByDataSetId + index))
        .check(status.is(expectedStatus))
    }

    def getDataSetsByDataSetIds(expectedStatus: Int, additionalHeaders: Map[String, String], dataSetReqs: HttpRequestBuilder*): HttpRequestBuilder = {
      http("Get DataSets By Data Set IDs")
        .get("/")
        .headers(TestConfig.jsonHeader)
        .headers(additionalHeaders)
        .resources(
          dataSetReqs:_*
        )

    }
  }

  object Dar {
    val darPartialResponse: String = "DAR_RESPONSE"
    val darRequestBody: String = "darRequestBody"
    val darPartialJson: String = "darPartialJson"
    val darReferenceId: String = "darReferenceId"
    val darId: String = "darId"
    val manageDarResponse: String = "manageDarResponse"
    val darConsentResponse: String = "darConsentResponse"
    val translateResponse: String = "translateResponse"

    def getPartial(expectedStatus: Int, referenceId: String, additionalHeaders: Map[String, String]): HttpRequestBuilder = {
      http("Get Partial Dar")
        .get("/api/dar/v2/" + referenceId)
        .headers(TestConfig.jsonHeader)
        .headers(additionalHeaders)
        .check(bodyString.saveAs(darPartialJson))
        .check(status.is(expectedStatus))
    }

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

    def updatePartial(expectedStatus: Int, referenceId: String, body: String, additionalHeaders: Map[String, String]): HttpRequestBuilder = {
      http("Update Partial Dar")
        .put("/api/dar/v2/draft/" + referenceId)
        .headers(TestConfig.jsonHeader)
        .headers(additionalHeaders)
        .body(StringBody(body)).asJson
        .check(bodyString.saveAs(darPartialJson))
        .check(status.is(expectedStatus))
    }

    def saveFinal(expectedStatus: Int, body: String, additionalHeaders: Map[String, String]): HttpRequestBuilder = {
      http("Submit Dar")
        .post("/api/dar/v2/")
        .headers(TestConfig.jsonHeader)
        .headers(additionalHeaders)
        .body(StringBody(body)).asJson
        .check(bodyString.saveAs(darPartialJson))
        .check(status.is(expectedStatus))
    }

    def manageDar(expectedStatus: Int, additionalHeaders: Map[String, String], userId: String = ""): HttpRequestBuilder = {
      http("Manage DARs")
        .get("/api/dar/manage/?userId=" + userId)
        .headers(TestConfig.jsonHeader)
        .headers(additionalHeaders)
        .check(bodyString.saveAs(manageDarResponse))
        .check(status.is(expectedStatus))

    }

    def getConsent(expectedStatus: Int, referenceId: String, additionalHeaders: Map[String, String]): HttpRequestBuilder = {
      http("Get DAR Consent")
        .get(s"api/dar/find/$referenceId/consent")
        .headers(TestConfig.jsonHeader)
        .headers(additionalHeaders)
        .check(bodyString.saveAs(darConsentResponse))
        .check(status.is(expectedStatus))
    }

    def translateDataUse(expectedStatus: Int, body: String, additionalHeaders: Map[String, String]): HttpRequestBuilder = {
      http("Translate Data Use")
        .post(s"${TestConfig.ontologyUrl}translate/summary")
        .headers(TestConfig.jsonHeader)
        .headers(additionalHeaders)
        .body(StringBody(body))
        .asJson
        .check(bodyString.saveAs(translateResponse))
        .check(status.is(expectedStatus))
    }
  }

  object Researcher {
    val researcherPropertiesResponse: String = "researcherPropertiesResponse"
    val researcherProfileResponse: String = "researcherProfileResponse"

    def getResearcherProperties(expectedStatus: Int, userId: String, additionalHeaders: Map[String, String]): HttpRequestBuilder = {
      http("Get Researcher Properties")
        .get("/api/researcher/" + userId)
        .headers(TestConfig.jsonHeader)
        .headers(additionalHeaders)
        .check(bodyString.saveAs(researcherPropertiesResponse))
        .check(status.is(expectedStatus))
    }
  }

  object FireCloud {
    val fireCloudVerifyResponse: String = "fireCloudVerifyResponse"
    val fireCloudVerifyStatus: String = "fireCloudVerifyStatus"
    val verifyTokenResponse: String = "verifyTokenResponse"
    val registerUserResponse: String = "registerUserResponse"
    val saveNihUserResponse: String = "saveNihUserResponse"

    def verifyUser(additionalHeaders: Map[String, String]): HttpRequestBuilder = {
      http("Verify User with FireCloud")
        .get(s"${TestConfig.fireCloudUrl}/me")
        .headers(TestConfig.jsonHeader)
        .headers(additionalHeaders)
        .check(bodyString.saveAs(fireCloudVerifyResponse))
        .check(status.saveAs(fireCloudVerifyStatus))
        .check(status.not(500))
    }

    def verifyToken(expectedStatus: Int, token: String, additionalHeaders: Map[String, String]): HttpRequestBuilder = {
      http("Verify eRA Token")
        .post(s"${TestConfig.profileUrl}/shibboleth-token")
        .headers(TestConfig.jsonHeader)
        .headers(additionalHeaders)
        .body(StringBody(token))
        .check(bodyString.saveAs(verifyTokenResponse))
        .check(status.is(expectedStatus))
    }

    def registerUser(expectedStatus: Int, body: String, additionalHeaders: Map[String, String]): HttpRequestBuilder = {
      http("Register FC User")
        .post(s"${TestConfig.fireCloudUrl}/register/profile")
        .headers(TestConfig.jsonHeader)
        .headers(additionalHeaders)
        .body(StringBody(body)).asJson
        .check(bodyString.saveAs(registerUserResponse))
        .check(status.is(expectedStatus))
    }

    def saveNihUser(expectedStatus: Int, body: String, additionalHeaders: Map[String, String]): HttpRequestBuilder = {
      http("Save Nih User")
        .post("/api/nih")
        .headers(TestConfig.jsonHeader)
        .headers(additionalHeaders)
        .body(StringBody(body)).asJson
        .check(bodyString.saveAs(saveNihUserResponse))
        .check(status.is(expectedStatus))
    }
  }

  object Admin {
    val unreviewedConsentResponse: String = "unreviewedConsentResponse"
    val unreviewedDarResponse: String = "unreviewedDarResponse"

    def unreviewedConsent(expectedStatus: Int, additionalHeaders: Map[String, String]): HttpRequestBuilder = {
      http("Get Unreviewed Consent")
        .get("/api/consent/unreviewed")
        .headers(TestConfig.jsonHeader)
        .headers(additionalHeaders)
        .check(bodyString.saveAs(unreviewedConsentResponse))
        .check(status.is(expectedStatus))
    }

    def unreviewedDar(expectedStatus: Int, additionalHeaders: Map[String, String]): HttpRequestBuilder = {
      http("Get Unreviewed DARs")
        .get("/api/dar/cases/unreviewed")
        .headers(TestConfig.jsonHeader)
        .headers(additionalHeaders)
        .check(bodyString.saveAs(unreviewedDarResponse))
        .check(status.is(expectedStatus))
    }

    def initConsole(expectedStatus: Int, additionalHeaders: Map[String, String]): HttpRequestBuilder = {
      http("Init Admin Console")
        .get("/")
        .resources(
          unreviewedConsent(expectedStatus, additionalHeaders),
          unreviewedDar(expectedStatus, additionalHeaders)
        )
    }
  }

  object Election {
    val createElectionResponse: String = "createElectionResponse"
    val getDataAccessElectionReviewResponse: String = "getElectionReviewResponse"

    def createElection(expectedStatus: Int, dataRequestId: String, body: String, additionalHeaders: Map[String, String]): HttpRequestBuilder = {
      http("Create Elections")
        .post(s"api/dataRequest/$dataRequestId/election")
        .headers(TestConfig.jsonHeader)
        .headers(additionalHeaders)
        .body(StringBody(body)).asJson
        .check(bodyString.saveAs(createElectionResponse))
        .check(status.is(expectedStatus))
    }

    def getDataAccessElectionReview(expectedStatus: Int, electionId: String, isFinalAccess: String, additionalHeaders: Map[String, String]): HttpRequestBuilder = {
      http("Get Election Review")
        .get(s"api/electionReview/access/$electionId?isFinalVote=$isFinalAccess")
        .headers(TestConfig.jsonHeader)
        .headers(additionalHeaders)
        .check(bodyString.saveAs(getDataAccessElectionReviewResponse))
        .check(status.is(expectedStatus))
    }
  }

  object PendingCases {
    val consentPendingResponse: String = "consentPendingResponse"
    val dataRequestPendingResponse: String = "dataRequestPendingResponse"

    def getPendingCasesByUserId(expectedStatus: Int, userId: String, additionalHeaders: Map[String, String]): HttpRequestBuilder = {
      http("Get Pending Cases")
        .get(s"api/consent/cases/pending/$userId")
        .headers(TestConfig.jsonHeader)
        .headers(additionalHeaders)
        .check(bodyString.saveAs(consentPendingResponse))
        .check(status.is(expectedStatus))
    }

    def getPendingDataRequestsByUserId(expectedStatus: Int, userId: String, additionalHeaders: Map[String, String]): HttpRequestBuilder = {
      http("Get Pending Data Requests")
        .get(s"api/dataRequest/cases/pending/$userId")
        .headers(TestConfig.jsonHeader)
        .headers(additionalHeaders)
        .check(bodyString.saveAs(dataRequestPendingResponse))
        .check(status.is(expectedStatus))
    }
  }

  object Votes {
    val getDarVoteResponse: String = "getDarVotesResponse"
    val darElectionId: String = "darElectionId"
    val getDarVoteListResponse: String = "getDarVoteListResponse"
    val postDarVoteResponse: String = "postDarVoteResponse"
    val updateDarVoteResponse: String = "updateDarVoteResponse"

    def getDarVote(expectedStatus: Int, requestId: String, voteId: String, additionalHeaders: Map[String, String]): HttpRequestBuilder = {
      http("Get DAR Vote")
        .get(s"api/dataRequest/$requestId/vote/$voteId")
        .headers(TestConfig.jsonHeader)
        .headers(additionalHeaders)
        .check(bodyString.saveAs(getDarVoteResponse))
        .check(jsonPath("$.electionId").saveAs(darElectionId))
        .check(status.is(expectedStatus))
    }

    def getDarVoteList(expectedStatus: Int, requestId: String, additionalHeaders: Map[String, String]): HttpRequestBuilder = {
      http("Get DAR Votes")
        .get(s"api/dataRequest/$requestId/vote")
        .headers(TestConfig.jsonHeader)
        .headers(additionalHeaders)
        .check(bodyString.saveAs(getDarVoteListResponse))
        .check(status.is(expectedStatus))
    }

    def postDarVote(expectedStatus: Int, requestId: String, voteId: String, body: String, additionalHeaders: Map[String, String]): HttpRequestBuilder = {
      http("Post DAR Vote")
        .post(s"api/dataRequest/$requestId/vote/$voteId")
        .headers(TestConfig.jsonHeader)
        .headers(additionalHeaders)
        .body(StringBody(body))
        .asJson
        .check(bodyString.saveAs(postDarVoteResponse))
        .check(status.is(expectedStatus))
    }

    def updateDarVote(expectedStatus: Int, requestId: String, voteId: String, body: String, additionalHeaders: Map[String, String]): HttpRequestBuilder = {
      http("Update DAR Vote")
        .put(s"api/dataRequest/$requestId/vote/$voteId")
        .headers(TestConfig.jsonHeader)
        .headers(additionalHeaders)
        .body(StringBody(body))
        .asJson
        .check(bodyString.saveAs(updateDarVoteResponse))
        .check(status.is(expectedStatus))
    }
  }
}

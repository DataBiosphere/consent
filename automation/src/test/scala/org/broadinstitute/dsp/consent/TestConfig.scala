package org.broadinstitute.dsp.consent

import java.io.FileInputStream

import com.google.auth.oauth2.{AccessToken, GoogleCredentials, ServiceAccountCredentials}
import com.typesafe.config.{Config, ConfigFactory}
import io.gatling.core.Predef._
import io.gatling.http.Predef.http
import io.gatling.http.protocol.HttpProtocolBuilder

import scala.collection.JavaConverters._
import scala.concurrent.duration._

object TestConfig {

  val config: Config = ConfigFactory.load()
  val defaultUsers: Int = 1
  val defaultPause: FiniteDuration = 1 second
  val defaultUserAgent: String = "Gatling Client"
  val plainTextHeader: Map[String, String] = Map("Accept" -> "text/plain")
  val jsonHeader: Map[String, String] = Map("Accept" -> "application/json")

  lazy val defaultHttpProtocol: HttpProtocolBuilder = {
    http
      .baseUrl(config.getString("consent.baseUrl"))
      .userAgentHeader(defaultUserAgent)
  }

  def getAccessToken(credentialFile: String): String = {
    val credential: GoogleCredentials = ServiceAccountCredentials.fromStream(
      new FileInputStream(getClass.getResource(credentialFile).getFile)).
      createScoped(List(
        "https://www.googleapis.com/auth/userinfo.profile",
        "https://www.googleapis.com/auth/userinfo.email").asJava)
    credential.refreshIfExpired()
    val token: AccessToken = credential.getAccessToken
    token.getTokenValue
  }

  lazy val adminHeader: Map[String, String] = Map("Authorization" -> s"Bearer ${getAccessToken("/accounts/duos-automation-admin.json")}")
  lazy val chairHeader: Map[String, String] = Map("Authorization" -> s"Bearer ${getAccessToken("/accounts/duos-automation-chair.json")}")
  lazy val memberHeader: Map[String, String] = Map("Authorization" -> s"Bearer ${getAccessToken("/accounts/duos-automation-member.json")}")
  lazy val researcherHeader: Map[String, String] = Map("Authorization" -> s"Bearer ${getAccessToken("/accounts/duos-automation-researcher.json")}")

}

package org.broadinstitute.dsp.consent

import java.io.FileInputStream

import com.google.auth.oauth2.{AccessToken, GoogleCredentials, ServiceAccountCredentials}
import com.typesafe.config.{Config, ConfigFactory}
import io.gatling.core.Predef._
import io.gatling.http.Predef.http
import io.gatling.http.protocol.HttpProtocolBuilder

import scala.jdk.CollectionConverters._
import scala.concurrent.duration._

object TestConfig {

  val config: Config = ConfigFactory.load()
  val defaultUsers: Int = 1
  val defaultPause: FiniteDuration = 1 second
  val defaultUserAgent: String = "Gatling Client"
  val plainTextHeader: Map[String, String] = Map("Accept" -> "text/plain")
  val jsonHeader: Map[String, String] = Map("Accept" -> "application/json")
  val jsonBodyHeader: Map[String, String] = Map("Content-Type" -> "application/json")

  lazy val baseUrl: String = {
    val envBase: Option[String] = sys.env.get("CONSENT_API_URL")
    
    if (envBase == None) {
      config.getString("consent.baseUrl")
    } else {
      envBase.getOrElse("")
    }
  }

  lazy val defaultHttpProtocol: HttpProtocolBuilder = {
    http
      .baseUrl(baseUrl)
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

  lazy val fireCloudUrl: String = config.getString("consent.fireCloudUrl")
  lazy val profileUrl: String = config.getString("consent.profileUrl")
  lazy val ontologyUrl: String = config.getString("consent.ontologyUrl")
  lazy val adminHeader: Map[String, String] = Map("Authorization" -> s"Bearer ${getAccessToken("/accounts/duos-automation-admin.json")}")
  lazy val chairHeader: Map[String, String] = Map("Authorization" -> s"Bearer ${getAccessToken("/accounts/duos-automation-chair.json")}")
  lazy val memberHeader: Map[String, String] = Map("Authorization" -> s"Bearer ${getAccessToken("/accounts/duos-automation-member.json")}")
  lazy val researcherHeader: Map[String, String] = Map("Authorization" -> s"Bearer ${getAccessToken("/accounts/duos-automation-researcher.json")}")

}

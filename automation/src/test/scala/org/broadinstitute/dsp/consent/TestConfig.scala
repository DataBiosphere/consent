package org.broadinstitute.dsp.consent

import com.typesafe.config.{Config, ConfigFactory}
import io.gatling.core.Predef._
import io.gatling.http.Predef.http
import io.gatling.http.protocol.HttpProtocolBuilder

import scala.concurrent.duration._

object TestConfig {

  val config: Config = ConfigFactory.load()
  val defaultUsers: Int = 1
  val defaultPause: FiniteDuration = 1 second
  val defaultUserAgent: String = "Gatling Client"
  val plainTextHeader: Map[String, String] = Map("Accept" -> "text/plain")
  val jsonHeader: Map[String, String] = Map("Accept" -> "application/json")
  lazy val adminHeader: Map[String, String] = Map("Authorization" -> s"Bearer ${config.getString("consent.adminAccessToken")}")
  lazy val chairHeader: Map[String, String] = Map("Authorization" -> s"Bearer ${config.getString("consent.chairAccessToken")}")
  lazy val memberHeader: Map[String, String] = Map("Authorization" -> s"Bearer ${config.getString("consent.memberAccessToken")}")
  lazy val researcherHeader: Map[String, String] = Map("Authorization" -> s"Bearer ${config.getString("consent.researcherAccessToken")}")

  lazy val defaultHttpProtocol: HttpProtocolBuilder = {
    http
      .baseUrl(config.getString("consent.baseUrl"))
      .userAgentHeader(defaultUserAgent)
  }

}

import sbt._

object Dependencies {

  val gatlingV = "3.4.1"

  private val netty = "io.netty" % "netty-codec-http" % "4.1.56.Final"
  private val nettyHandler = netty.organization % "netty-handler" % netty.revision

  val rootDependencies: Seq[ModuleID] = Seq(
    "org.scalatest"        %% "scalatest" % "3.2.0" % Test,
    netty,
    nettyHandler,
    "io.gatling"            % "gatling-core" % gatlingV,
    "io.gatling"            % "gatling-http" % gatlingV,
    "io.gatling.highcharts" % "gatling-charts-highcharts" % gatlingV % "test",
    "io.gatling"            % "gatling-test-framework"    % gatlingV % "test",
    "com.google.auth"       % "google-auth-library-oauth2-http" % "0.19.0",
    "io.spray"             %%  "spray-json" % "1.3.6"
  )

}

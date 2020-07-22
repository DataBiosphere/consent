import sbt._

object Dependencies {

  val gatlingV = "3.3.1"

  val rootDependencies: Seq[ModuleID] = Seq(
    "org.scalatest"        %% "scalatest" % "3.2.0" % Test,
    "io.gatling"            % "gatling-core" % gatlingV,
    "io.gatling"            % "gatling-http" % gatlingV,
    "io.gatling.highcharts" % "gatling-charts-highcharts" % gatlingV % "test",
    "io.gatling"            % "gatling-test-framework"    % gatlingV % "test",
    "com.google.auth"       % "google-auth-library-oauth2-http" % "0.19.0"
  )

}

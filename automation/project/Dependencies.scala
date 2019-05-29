import sbt._

object Dependencies {

  val gatlingV = "3.0.3"

  val rootDependencies: Seq[ModuleID] = Seq(
    "org.scalatest"        %% "scalatest" % "3.0.5" % Test,
    "io.gatling"            % "gatling-core" % gatlingV,
    "io.gatling"            % "gatling-http" % gatlingV,
    "io.gatling.highcharts" % "gatling-charts-highcharts" % gatlingV % "test",
    "io.gatling"            % "gatling-test-framework"    % gatlingV % "test"
  )

}

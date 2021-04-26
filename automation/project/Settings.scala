import Dependencies._
import sbt.Keys._
import sbt.{Def, _}

object Settings {

  val commonBuildSettings: Seq[Def.Setting[_]] = Defaults.coreDefaultSettings ++ Defaults.defaultConfigs ++ Seq(
    javaOptions += "-Xmx2G",
    javacOptions ++= Seq("-source", "11", "-target", "11")
  )

  val commonCompilerSettings: Seq[String] = Seq(
    "-unchecked",
    "-deprecation",
    "-feature",
    "-encoding", "utf8",
    "-language:postfixOps",
    "-target:11"
  )

  //common settings for all sbt subprojects
  val commonSettings: Seq[Def.Setting[_]] =
    commonBuildSettings ++ List(
      organization  := "org.broadinstitute.dsp.consent",
      scalaVersion  := "2.13.5",
      scalacOptions ++= commonCompilerSettings
    )

  lazy val rootSettings: Seq[Def.Setting[_]] = commonSettings ++ List(
    name := "consent",
    libraryDependencies ++= rootDependencies
  )

}

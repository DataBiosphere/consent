import Dependencies._
import sbt.Keys._
import sbt.{Def, _}

object Settings {

  val commonBuildSettings: Seq[Def.Setting[_]] = Defaults.coreDefaultSettings ++ Defaults.defaultConfigs ++ Seq(
    javaOptions += "-Xmx2G",
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8")
  )

  val commonCompilerSettings: Seq[String] = Seq(
    "-unchecked",
    "-deprecation",
    "-feature",
    "-encoding", "utf8",
    "-language:postfixOps",
    "-target:jvm-1.8",
    "-Xmax-classfile-name", "100",
    "-Ypartial-unification" // Enable partial unification in type constructor inference
  )

  //common settings for all sbt subprojects
  val commonSettings: Seq[Def.Setting[_]] =
    commonBuildSettings ++ List(
      organization  := "org.broadinstitute.dsp.consent",
      scalaVersion  := "2.12.7",
      scalacOptions ++= commonCompilerSettings
    )

  lazy val rootSettings: Seq[Def.Setting[_]] = commonSettings ++ List(
    name := "consent",
    libraryDependencies ++= rootDependencies
  )

}

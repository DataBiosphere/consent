enablePlugins(GatlingPlugin)

import Settings.rootSettings
import sbt._

lazy val root = (project in file(".")).settings(rootSettings:_*)

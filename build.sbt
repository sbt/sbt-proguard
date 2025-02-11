ThisBuild / version := {
  if ((ThisBuild / isSnapshot).value) "0.5.0" + "-SNAPSHOT"
  else (ThisBuild / version).value
}
ThisBuild / scalaVersion := "2.12.20"

lazy val root = (project in file("."))
  .enablePlugins(SbtPlugin)
  .settings(nocomma {
    name := "sbt-proguard"

    scalacOptions ++= Seq(
      "-unchecked",
      "-deprecation",
      "-feature",
      "-Xlint",
      "-Xfatal-warnings",
    )

    scriptedDependencies := publishLocal.value
    scriptedLaunchOpts ++= Seq("-Xms512m", "-Xmx512m", s"-Dproject.version=${version.value}")
    // scriptedBufferLog := false

    (pluginCrossBuild / sbtVersion) := {
      scalaBinaryVersion.value match {
        case "2.12" => "1.2.8"
        case _      => "2.0.0-M3"
      }
    }
  })

Global / onChangedBuildSource := ReloadOnSourceChanges
ThisBuild / organization := "com.github.sbt"
ThisBuild / description := "an sbt plugin for Proguard"
ThisBuild / homepage := Some(url("https://github.com/sbt/sbt-proguard"))
ThisBuild / licenses := List("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.txt"))
ThisBuild / developers := List(
  Developer(
    "pvlugter",
    "Peter Vlugter",
    "@pvlugter",
    url("https://github.com/pvlugter")
  )
)
ThisBuild / publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
ThisBuild / dynverSonatypeSnapshots := true

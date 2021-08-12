sbtPlugin := true
enablePlugins(SbtPlugin)

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "1.0.0")
enablePlugins(AssemblyPlugin)

organization := "com.lightbend.sbt"
name := "sbt-proguard"
publishMavenStyle := false

bintrayOrganization := Some("sbt")
bintrayRepository := "sbt-plugin-releases"
bintrayPackage := name.value
bintrayReleaseOnPublish := false

scriptedDependencies := publishLocal.value
scriptedLaunchOpts ++= Seq("-Xms512m", "-Xmx512m", s"-Dproject.version=${version.value}")
//scriptedBufferLog := false

import ReleaseTransformations._
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  releaseStepCommandAndRemaining("scripted"),
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommandAndRemaining("publish"),
  releaseStepTask(bintrayRelease),
  setNextVersion,
  commitNextVersion,
  pushChanges
)

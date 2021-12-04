sbtPlugin := true
enablePlugins(SbtPlugin)

organization := "com.lightbend.sbt"
homepage := Some(url("https://github.com/sbt/sbt-proguard"))
name := "sbt-proguard"
publishMavenStyle := false

scriptedDependencies := publishLocal.value
scriptedLaunchOpts ++= Seq("-Xms512m", "-Xmx512m", s"-Dproject.version=${version.value}")
//scriptedBufferLog := false

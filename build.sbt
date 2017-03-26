
sbtPlugin := true

organization := "com.typesafe.sbt"
name := "sbt-proguard"
version := "0.2.4-SNAPSHOT"

publishMavenStyle := false

bintrayOrganization := Some("sbt")
bintrayRepository := "sbt-plugin-releases"
bintrayPackage := name.value
bintrayReleaseOnPublish := false

crossBuildingSettings
CrossBuilding.crossSbtVersions := Seq("0.12", "0.13")
CrossBuilding.scriptedSettings

scriptedLaunchOpts := Seq("-Xms512m", "-Xmx512m", "-XX:MaxPermSize=256m", s"-Dproject.version=${version.value}")

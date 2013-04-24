
sbtPlugin := true

organization := "com.typesafe.sbt"

name := "sbt-proguard"

version := "0.2.1"

publishMavenStyle := false

publishTo <<= (version) { v =>
  def scalasbt(repo: String) = ("scalasbt " + repo, "http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-" + repo)
  val (name, repo) = if (v.endsWith("-SNAPSHOT")) scalasbt("snapshots") else scalasbt("releases")
  Some(Resolver.url(name, url(repo))(Resolver.ivyStylePatterns))
}

ScriptedPlugin.scriptedSettings

scriptedLaunchOpts := Seq("-Xms512m", "-Xmx512m", "-XX:MaxPermSize=256m")

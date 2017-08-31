addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.5.1")
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.6")

libraryDependencies += "org.scala-sbt" % "scripted-plugin" % sbtVersion.value

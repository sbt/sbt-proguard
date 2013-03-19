
resolvers <+= sbtResolver

libraryDependencies <+= sbtVersion { v => "org.scala-sbt" % "scripted-plugin" % v }

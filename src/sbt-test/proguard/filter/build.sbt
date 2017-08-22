enablePlugins(SbtProguard)

options in Proguard ++= Seq("-dontnote", "-dontwarn", "-ignorewarnings")

options in Proguard += ProguardOptions.keepMain("Test")

inputFilter in Proguard := { file =>
  file.name match {
    case "scala-library.jar" => Some("!META-INF/**")
    case _                   => None
  }
}

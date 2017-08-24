enablePlugins(SbtProguard)

scalaVersion := "2.10.6"

proguardOptions in Proguard ++= Seq("-dontnote", "-dontwarn", "-ignorewarnings")

proguardOptions in Proguard += ProguardOptions.keepMain("Test")

proguardInputFilter in Proguard := { file =>
  file.name match {
    case "scala-library.jar" => Some("!META-INF/**")
    case _                   => None
  }
}

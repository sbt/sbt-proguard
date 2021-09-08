import java.nio.file.FileSystems

enablePlugins(SbtProguard)

scalaVersion := "2.13.6"
name := "filter"

(Proguard / proguardMerge) := true
(Proguard / proguardOptions) ++= Seq("-dontoptimize", "-dontnote", "-dontwarn", "-ignorewarnings")
(Proguard / proguardOptions) += ProguardOptions.keepMain("Test")
(Proguard / proguardInputFilter) := { file =>
  file.name match {
    case "scala-library-2.13.6.jar" => Some("!META-INF/**")
    case _                   => None
  }
}

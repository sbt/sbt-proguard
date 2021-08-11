import java.nio.file.FileSystems

enablePlugins(SbtProguard)

scalaVersion := "2.12.3"
name := "simple"

(Proguard / proguardOptions) ++= Seq("-dontoptimize", "-dontnote", "-dontwarn", "-ignorewarnings")
(Proguard / proguardOptions) += ProguardOptions.keepMain("Test")

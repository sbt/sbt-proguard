import java.nio.file.FileSystems

enablePlugins(SbtProguard)

scalaVersion := "2.13.6"
name := "simple"

(Proguard / proguardOptions) ++= Seq("-dontoptimize", "-dontnote", "-dontwarn", "-ignorewarnings")
(Proguard / proguardOptions) += ProguardOptions.keepMain("Test")

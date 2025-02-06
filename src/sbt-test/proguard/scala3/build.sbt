import java.nio.file.FileSystems

enablePlugins(SbtProguard)

scalaVersion := "3.6.3"
name := "scala3"

Proguard / proguardOptions ++= Seq("-dontoptimize", "-dontnote", "-dontwarn", "-ignorewarnings")
Proguard / proguardOptions += ProguardOptions.keepMain("Test")

Proguard / proguardInputs := (Compile / dependencyClasspath).value.files

Proguard / proguardFilteredInputs ++= ProguardOptions.noFilter((Compile / packageBin).value)

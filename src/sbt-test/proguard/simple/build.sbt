import java.nio.file.FileSystems

enablePlugins(SbtProguard)

scalaVersion := "2.13.6"
name := "simple"

Proguard / proguardOptions ++= Seq("-dontoptimize", "-dontnote", "-dontwarn", "-ignorewarnings")
Proguard / proguardOptions += ProguardOptions.keepMain("Test")

Proguard / proguardInputs := (Compile / dependencyClasspath).value.files

Proguard / proguardFilteredInputs ++= ProguardOptions.noFilter((Compile / packageBin).value)

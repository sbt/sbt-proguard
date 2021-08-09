enablePlugins(SbtProguard)

scalaVersion := "2.12.3"

(Proguard / proguardOptions) += "-dontoptimize"

(Proguard / proguardOptions) ++= Seq("-dontnote", "-verbose")

(Proguard / proguardOptions) += ProguardOptions.keepMain("Test")

(Proguard / proguardInputs) := (Compile / dependencyClasspath).value.files

(Proguard / proguardFilteredInputs) ++= ProguardOptions.noFilter((Compile / packageBin).value)

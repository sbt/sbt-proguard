enablePlugins(SbtProguard)

scalaVersion := "2.10.6"

proguardOptions in Proguard ++= Seq("-dontnote", "-dontwarn", "-ignorewarnings")

proguardOptions in Proguard += ProguardOptions.keepMain("Test")

proguardInputs in Proguard := (dependencyClasspath in Compile).value.files

proguardFilteredInputs in Proguard ++= ProguardOptions.noFilter((packageBin in Compile).value)

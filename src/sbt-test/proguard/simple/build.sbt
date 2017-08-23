enablePlugins(SbtProguard)

options in Proguard ++= Seq("-dontnote", "-dontwarn", "-ignorewarnings")

options in Proguard += ProguardOptions.keepMain("Test")

inputs in Proguard := (dependencyClasspath in Compile).value.files

filteredInputs in Proguard ++= ProguardOptions.noFilter((packageBin in Compile).value)

proguardSettings

ProguardKeys.options in Proguard ++= Seq("-dontnote", "-dontwarn", "-ignorewarnings")

ProguardKeys.options in Proguard += ProguardOptions.keepMain("Test")

ProguardKeys.inputs in Proguard <<= (dependencyClasspath in Compile) map { _.files }

ProguardKeys.filteredInputs in Proguard <++= (packageBin in Compile) map ProguardOptions.noFilter

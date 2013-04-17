proguardSettings

assemblySettings

ProguardKeys.options in Proguard ++= Seq("-dontnote", "-dontwarn", "-ignorewarnings")

ProguardKeys.options in Proguard += ProguardOptions.keepMain("Test")

ProguardKeys.filteredInputs in Proguard <<= AssemblyKeys.assembly map ProguardOptions.noFilter

proguardSettings

assemblySettings

ProguardKeys.options in Proguard ++= Seq("-dontnote", "-dontwarn", "-ignorewarnings")

ProguardKeys.options in Proguard += ProguardOptions.keepMain("Test")

ProguardKeys.filteredInJars in Proguard <<= AssemblyKeys.assembly map { jar => Seq(FilteredJar(jar, None)) }

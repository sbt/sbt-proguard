enablePlugins(SbtProguard)

options in Proguard ++= Seq("-dontnote", "-dontwarn", "-ignorewarnings")

options in Proguard += ProguardOptions.keepMain("Test")

merge in Proguard := true

mergeStrategies in Proguard += ProguardMerge.discard("META-INF/.*".r)

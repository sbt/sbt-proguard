proguardSettings

ProguardKeys.options in Proguard ++= Seq("-dontnote", "-dontwarn", "-ignorewarnings")

ProguardKeys.options in Proguard += ProguardOptions.keepMain("Test")

ProguardKeys.merge in Proguard := true

ProguardKeys.mergeStrategies in Proguard += ProguardMerge.discard("META-INF/.*".r)

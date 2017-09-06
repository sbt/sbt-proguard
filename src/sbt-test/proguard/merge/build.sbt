enablePlugins(SbtProguard)

scalaVersion := "2.12.3"

proguardOptions in Proguard += "-dontoptimize"

proguardOptions in Proguard ++= Seq("-dontnote", "-dontwarn", "-ignorewarnings")

proguardOptions in Proguard += ProguardOptions.keepMain("Test")

proguardMerge in Proguard := true

proguardMergeStrategies in Proguard += ProguardMerge.discard("META-INF/.*".r)

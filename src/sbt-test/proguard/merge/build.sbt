enablePlugins(SbtProguard)

scalaVersion := "2.12.3"

(Proguard / proguardOptions) += "-dontoptimize"

(Proguard / proguardOptions) ++= Seq("-dontnote", "-dontwarn", "-ignorewarnings")

(Proguard / proguardOptions) += ProguardOptions.keepMain("Test")

(Proguard / proguardMerge) := true

(Proguard / proguardMergeStrategies) += ProguardMerge.discard("META-INF/.*".r)

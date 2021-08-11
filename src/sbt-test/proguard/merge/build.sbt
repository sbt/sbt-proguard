import java.nio.file.FileSystems

enablePlugins(SbtProguard)

scalaVersion := "2.12.3"
name := "merge"

(Proguard / proguardMerge) := true
(Proguard / proguardOptions) ++= Seq("-dontoptimize", "-dontnote", "-dontwarn", "-ignorewarnings")
(Proguard / proguardOptions) += ProguardOptions.keepMain("Test")
(Proguard / proguardMergeStrategies) += ProguardMerge.discard("META-INF/.*".r)

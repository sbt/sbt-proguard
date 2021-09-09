import java.nio.file.FileSystems

enablePlugins(SbtProguard)

scalaVersion := "2.13.6"
name := "filter"

(Proguard / proguardMerge) := true
(Proguard / proguardOptions) ++= Seq("-dontoptimize", "-dontnote", "-dontwarn", "-ignorewarnings")
(Proguard / proguardOptions) += ProguardOptions.keepMain("Test")
(Proguard / proguardInputFilter) := { file =>
   if (file.name == s"scala-library-${scalaVersion.value}.jar") 
     Some("!META-INF/**")
   else 
     None
}

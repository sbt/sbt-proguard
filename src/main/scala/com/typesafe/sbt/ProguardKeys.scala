package com.typesafe.sbt

import com.typesafe.sbt.SbtProguard.autoImport.ProguardOptions.Filtered
import com.typesafe.sbt.proguard.Merge
import com.typesafe.sbt.proguard.Merge.Strategy
import sbt.{File, SettingKey, _}

trait ProguardKeys {
  val proguardVersion = settingKey[String]("proguard version")
  val proguardDirectory = settingKey[File]("proguard directory")
  val proguardConfiguration = settingKey[File]("proguard configuration")
  val proguardBinaryDeps = taskKey[Seq[File]]("proguard binary dependencies")
  val proguardInputs = taskKey[Seq[File]]("proguard inputs")
  val proguardLibraries = taskKey[Seq[File]]("proguard libraries")
  val proguardOutputs = taskKey[Seq[File]]("proguard outputs")
  val proguardDefaultInputFilter = taskKey[Option[String]]("proguard default input filter")
  val proguardInputFilter = taskKey[File => Option[String]]("proguard input filter")
  val proguardLibraryFilter = taskKey[File => Option[String]]("proguard library filter")
  val proguardOutputFilter = taskKey[File => Option[String]]("proguard output filter")
  val proguardFilteredInputs = taskKey[Seq[Filtered]]("proguard filtered inputs")
  val proguardFilteredLibraries = taskKey[Seq[Filtered]]("proguard filtered libraries")
  val proguardFilteredOutputs = taskKey[Seq[Filtered]]("proguard filtered outputs")
  val proguardMerge = taskKey[Boolean]("proguard merge")
  val proguardMergeDirectory = settingKey[File]("proguard merge directory")
  val proguardMergeStrategies = taskKey[Seq[Strategy]]("proguard merge strategies")
  val proguardMergedInputs = taskKey[Seq[Filtered]]("proguard merged inputs")
  val proguardOptions = taskKey[Seq[String]]("proguard options")
  val proguard = taskKey[Seq[File]]("proguard")

  object ProguardOptions {

    case class Filtered(file: File, filter: Option[String])

    def noFilter(jar: File): Seq[Filtered] = Seq(Filtered(jar, None))

    def noFilter(jars: Seq[File]): Seq[Filtered] = filtered(jars, None)

    def filtered(jars: Seq[File], filter: File => Option[String]): Seq[Filtered] = {
      jars map { jar => Filtered(jar, filter(jar)) }
    }

    def filtered(jars: Seq[File], filter: Option[String]): Seq[Filtered] = {
      jars map { jar => Filtered(jar, filter) }
    }

    def filterString(filter: Option[String]): String = {
      filter map {
        "(" + _ + ")"
      } getOrElse ""
    }

    def jarOptions(option: String, jars: Seq[Filtered]): Seq[String] = {
      jars map { jar => "%s \"%s\"%s" format(option, jar.file.getCanonicalPath, filterString(jar.filter)) }
    }

    def keepMain(name: String): String = {
      """-keep public class %s {
        |    public static void main(java.lang.String[]);
        |}""".stripMargin.format(name)
    }
  }

  object ProguardMerge {

    import Merge.Strategy.{matchingRegex, matchingString}

    import scala.util.matching.Regex

    def defaultStrategies = Seq(
      discard("META-INF/MANIFEST.MF")
    )

    def discard(exactly: String) = matchingString(exactly, Merge.discard)
    def first(exactly: String) = matchingString(exactly, Merge.first)
    def last(exactly: String) = matchingString(exactly, Merge.last)
    def rename(exactly: String) = matchingString(exactly, Merge.rename)
    def append(exactly: String) = matchingString(exactly, Merge.append)

    def discard(pattern: Regex) = matchingRegex(pattern, Merge.discard)
    def first(pattern: Regex) = matchingRegex(pattern, Merge.first)
    def last(pattern: Regex) = matchingRegex(pattern, Merge.last)
    def rename(pattern: Regex) = matchingRegex(pattern, Merge.rename)
    def append(pattern: Regex) = matchingRegex(pattern, Merge.append)
  }

}

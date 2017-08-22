package com.typesafe.sbt

import com.typesafe.sbt.SbtProguard.autoImport.ProguardOptions.Filtered
import com.typesafe.sbt.proguard.Merge
import com.typesafe.sbt.proguard.Merge.Strategy
import sbt.{File, SettingKey, TaskKey}

trait ProguardKeys {
  val proguardVersion = SettingKey[String]("proguard-version")
  val proguardDirectory = SettingKey[File]("proguard-directory")
  val proguardConfiguration = SettingKey[File]("proguard-configuration")
  val binaryDeps = TaskKey[Seq[File]]("binaryDeps")
  val inputs = TaskKey[Seq[File]]("inputs")
  val libraries = TaskKey[Seq[File]]("libraries")
  val outputs = TaskKey[Seq[File]]("outputs")
  val defaultInputFilter = TaskKey[Option[String]]("default-input-filter")
  val inputFilter = TaskKey[File => Option[String]]("input-filter")
  val libraryFilter = TaskKey[File => Option[String]]("library-filter")
  val outputFilter = TaskKey[File => Option[String]]("output-filter")
  val filteredInputs = TaskKey[Seq[Filtered]]("filtered-inputs")
  val filteredLibraries = TaskKey[Seq[Filtered]]("filtered-libraries")
  val filteredOutputs = TaskKey[Seq[Filtered]]("filtered-outputs")
  val merge = TaskKey[Boolean]("merge")
  val mergeDirectory = SettingKey[File]("merge-directory")
  val mergeStrategies = TaskKey[Seq[Strategy]]("merge-strategies")
  val mergedInputs = TaskKey[Seq[Filtered]]("merged-inputs")
  val options = TaskKey[Seq[String]]("options")
  val proguard = TaskKey[Seq[File]]("proguard")

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

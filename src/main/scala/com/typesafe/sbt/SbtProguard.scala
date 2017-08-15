package com.typesafe.sbt

import com.typesafe.sbt.proguard.Merge
import sbt.Keys._
import sbt.internal.inc.Analysis
import sbt.{Def, _}

import scala.sys.process.Process

object SbtProguard extends AutoPlugin {

  val Proguard = config("proguard").hide

  object autoImport {

    import Merge.Strategy
    import ProguardOptions.Filtered

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
  }

  override lazy val projectSettings: Seq[Setting[_]] =
    inConfig(Proguard)(ProguardSettings.default) ++ ProguardSettings.dependencies

  object ProguardSettings {

    import autoImport._
    import ProguardOptions._

    def default: Seq[Setting[_]] = Seq(
      proguardVersion := "5.3.3",
      proguardDirectory := crossTarget.value / "proguard",
      proguardConfiguration := proguardDirectory.value / "configuration.pro",
      artifactPath := proguardDirectory.value / (artifactPath in packageBin in Compile).value.getName,
      managedClasspath := Classpaths.managedJars(configuration.value, classpathTypes.value, update.value),
      binaryDeps := (compile in Compile).value.asInstanceOf[Analysis].relations.allLibraryDeps.toSeq, // TODO: get relations safe
      inputs := (fullClasspath in Runtime).value.files,
      libraries := binaryDeps.value filterNot inputs.value.toSet,
      outputs := Seq(artifactPath.value),
      defaultInputFilter := Some("!META-INF/MANIFEST.MF"),
      inputFilter := {
        val defaultInputFilterValue = defaultInputFilter.value
        _ => defaultInputFilterValue
      },
      libraryFilter := { f => None },
      outputFilter := { f => None },
      filteredInputs := filtered(inputs.value, inputFilter.value),
      filteredLibraries := filtered(libraries.value, libraryFilter.value),
      filteredOutputs := filtered(outputs.value, outputFilter.value),
      merge := false,
      mergeDirectory := proguardDirectory.value / "merged",
      mergeStrategies := ProguardMerge.defaultStrategies,
      mergedInputs := mergeTask.value,
      options := {
        jarOptions("-injars", mergedInputs.value) ++
          jarOptions("-libraryjars", filteredLibraries.value) ++
          jarOptions("-outjars", filteredOutputs.value)
      },
      javaOptions in proguard := Seq("-Xmx256M"),
      autoImport.proguard := proguardTask.value
    )

    def dependencies: Seq[Setting[_]] = Seq(
      ivyConfigurations += Proguard,
      libraryDependencies += "net.sf.proguard" % "proguard-base" % (proguardVersion in Proguard).value % Proguard.name
    )

    val mergeTask = Def.task {
      val streamsValue = streams.value
      val mergeDirectoryValue = mergeDirectory.value
      val mergeStrategiesValue = mergeStrategies.value
      val filteredInputsValue = filteredInputs.value
      if (merge.value) {
        val cachedMerge = FileFunction.cached(streamsValue.cacheDirectory / "proguard-merge", FilesInfo.hash) { _ =>
          streamsValue.log.info("Merging inputs before proguard...")
          IO.delete(mergeDirectoryValue)
          val inputs = filteredInputsValue map (_.file)
          Merge.merge(inputs, mergeDirectoryValue, mergeStrategiesValue.reverse, streamsValue.log)
          mergeDirectoryValue.allPaths.get.toSet
        }
        val inputs = inputFiles(filteredInputsValue).toSet
        cachedMerge(inputs)
        val filters = (filteredInputsValue flatMap (_.filter)).toSet
        val combinedFilter = if (filters.nonEmpty) Some(filters.mkString(",")) else None
        Seq(Filtered(mergeDirectoryValue, combinedFilter))
      } else filteredInputsValue
    }

    val proguardTask = Def.task {
      writeConfiguration(proguardConfiguration.value, options.value)
      val proguardConfigurationValue = proguardConfiguration.value
      val javaOptionsInProguardValue = (javaOptions in proguard).value
      val managedClasspathValue = managedClasspath.value
      val streamsValue = streams.value
      val outputsValue = outputs.value
      val cachedProguard = FileFunction.cached(streams.value.cacheDirectory / "proguard", FilesInfo.hash) { _ =>
        outputsValue foreach IO.delete
        streamsValue.log.debug("Proguard configuration:")
        options.value foreach (streamsValue.log.debug(_))
        runProguard(proguardConfigurationValue, javaOptionsInProguardValue, managedClasspathValue.files, streamsValue.log)
        outputsValue.toSet
      }
      val inputs = (proguardConfiguration.value +: inputFiles(filteredInputs.value)).toSet
      cachedProguard(inputs)
      outputs.value
    }

    def inputFiles(inputs: Seq[Filtered]): Seq[File] = {
      inputs flatMap { i => if (i.file.isDirectory) i.file.allPaths.get else Seq(i.file) }
    }

    def writeConfiguration(config: File, options: Seq[String]): Unit = {
      IO.writeLines(config, options)
    }

    def runProguard(config: File, javaOptions: Seq[String], classpath: Seq[File], log: Logger): Unit = {
      val options = javaOptions ++ Seq("-cp", Path.makeString(classpath), "proguard.ProGuard", "-include", config.getAbsolutePath)
      log.debug("Proguard command:")
      log.debug("java " + options.mkString(" "))
      val exitCode = Process("java", options) ! log
      if (exitCode != 0) sys.error("Proguard failed with exit code [%s]" format exitCode)
    }
  }

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

package com.typesafe.sbt

import com.typesafe.sbt.proguard.Merge
import com.typesafe.sbt.proguard.Sbt10Compat._
import sbt.Keys._
import sbt.{Def, _}

import scala.sys.process.Process

object SbtProguard extends AutoPlugin {

  object autoImport extends ProguardKeys {
    lazy val Proguard: Configuration = config("proguard").hide

    //lazy val ProguardKeys = autoImport // for backwards compatibility
  }

  import autoImport._
  import ProguardOptions._

  override def projectConfigurations: Seq[Configuration] = Seq(Proguard)

  override lazy val projectSettings: Seq[Setting[_]] = inConfig(Proguard)(baseSettings) ++ dependencies

  def baseSettings: Seq[Setting[_]] = Seq(
    proguardVersion := "5.0",
    proguardDirectory := crossTarget.value / "proguard",
    proguardConfiguration := proguardDirectory.value / "configuration.pro",
    artifactPath := proguardDirectory.value / (artifactPath in packageBin in Compile).value.getName,
    managedClasspath := Classpaths.managedJars(configuration.value, classpathTypes.value, update.value),
    binaryDeps := getAllBinaryDeps.value,
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
    libraryDependencies += "net.sf.proguard" % "proguard-base" % (proguardVersion in Proguard).value % Proguard
  )

  lazy val mergeTask: Def.Initialize[Task[Seq[ProguardOptions.Filtered]]] = Def.task {
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

  lazy val proguardTask: Def.Initialize[Task[Seq[File]]] = Def.task {
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

  def inputFiles(inputs: Seq[Filtered]): Seq[File] =
    inputs flatMap { i => if (i.file.isDirectory) i.file.allPaths.get else Seq(i.file) }

  def writeConfiguration(config: File, options: Seq[String]): Unit =
    IO.writeLines(config, options)

  def runProguard(config: File, javaOptions: Seq[String], classpath: Seq[File], log: Logger): Unit = {
    require(classpath.nonEmpty, "Proguard classpath cannot be empty!")
    val options = javaOptions ++ Seq("-cp", Path.makeString(classpath), "proguard.ProGuard", "-include", config.getAbsolutePath)
    log.debug("Proguard command:")
    log.debug("java " + options.mkString(" "))
    val exitCode = Process("java", options) ! log
    if (exitCode != 0) sys.error("Proguard failed with exit code [%s]" format exitCode)
  }
}

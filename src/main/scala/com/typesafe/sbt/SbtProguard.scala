package com.lightbend.sbt

import com.lightbend.sbt.proguard.Merge
import sbt.Keys._
import sbt._
import java.nio.file.{Files, FileSystems}
import scala.sys.process.Process

object SbtProguard extends AutoPlugin {

  object autoImport extends ProguardKeys {
    lazy val Proguard: Configuration = config("proguard").hide
  }

  import autoImport._
  import ProguardOptions._

  override def requires: Plugins = plugins.JvmPlugin

  override def trigger = allRequirements

  override def projectConfigurations: Seq[Configuration] = Seq(Proguard)

  override lazy val projectSettings: Seq[Setting[_]] = inConfig(Proguard)(baseSettings) ++ dependencies

  def baseSettings: Seq[Setting[_]] = Seq(
    proguardVersion := "7.0.0",
    proguardDirectory := crossTarget.value / "proguard",
    proguardConfiguration := proguardDirectory.value / "configuration.pro",
    artifactPath := proguardDirectory.value / ( Compile / packageBin / artifactPath).value.getName,
    managedClasspath := Classpaths.managedJars(configuration.value, classpathTypes.value, update.value),
    proguardInputs := (Runtime/fullClasspath).value.files,
    (proguard / javaHome) := Some(FileSystems.getDefault.getPath(System.getProperty("java.home")).toFile), 
    proguardLibraries := {
      val dependencyJars = (Compile / dependencyClasspathAsJars).value.map(_.data)
      dependencyJars.filterNot(proguardInputs.value.toSet) ++ (proguard / javaHome).value
    },
    proguardOutputs := Seq(artifactPath.value),
    proguardDefaultInputFilter := Some("!META-INF/MANIFEST.MF"),
    proguardInputFilter := {
      val defaultInputFilterValue = proguardDefaultInputFilter.value
      _ => defaultInputFilterValue
    },
    proguardLibraryFilter := { _ => None },
    proguardOutputFilter := { _ => None },
    proguardFilteredInputs := filtered(proguardInputs.value, proguardInputFilter.value),
    proguardFilteredLibraries := filtered(proguardLibraries.value, proguardLibraryFilter.value),
    proguardFilteredOutputs := filtered(proguardOutputs.value, proguardOutputFilter.value),
    proguardMerge := false,
    proguardMergeDirectory := proguardDirectory.value / "merged",
    proguardMergeStrategies := ProguardMerge.defaultStrategies,
    proguardMergedInputs := mergeTask.value,
    proguardOptions := {
      jarOptions("-injars", proguardMergedInputs.value) ++
        jarOptions("-libraryjars", proguardFilteredLibraries.value) ++
        jarOptions("-outjars", proguardFilteredOutputs.value)
    },
    proguardRemoveTastyFiles := true,
    proguard / javaOptions := Seq("-Xmx256M"),
    autoImport.proguard := proguardTask.value,
  )

  private def groupId(proguardVersionStr: String): String =
    "^(\\d+)\\.".r
      .findFirstMatchIn(proguardVersionStr)
      .map(_.group(1).toInt) match {
      case Some(v) => if (v > 6) "com.guardsquare" else "net.sf.proguard"
      case None => sys.error(s"Can't parse Proguard version: $proguardVersion")
    }

  def dependencies: Seq[Setting[_]] = Seq(
    resolvers += Resolver.bintrayRepo("guardsquare", "proguard"),
    libraryDependencies += groupId((Proguard / proguardVersion).value) % "proguard-base" % (Proguard / proguardVersion).value % Proguard
  )

  lazy val mergeTask: Def.Initialize[Task[Seq[ProguardOptions.Filtered]]] = Def.task {
    val streamsValue = streams.value
    val mergeDirectoryValue = proguardMergeDirectory.value
    val mergeStrategiesValue = proguardMergeStrategies.value
    val filteredInputsValue = proguardFilteredInputs.value
    if (!proguardMerge.value) filteredInputsValue
    else {
      val cachedMerge = FileFunction.cached(streamsValue.cacheDirectory / "proguard-merge", FilesInfo.hash) { _ =>
        streamsValue.log.info("Merging inputs before proguard...")
        IO.delete(mergeDirectoryValue)
        IO.createDirectory(mergeDirectoryValue)
        val inputs = filteredInputsValue map (_.file)
        Merge.merge(inputs, mergeDirectoryValue, mergeStrategiesValue.reverse, streamsValue.log)
        mergeDirectoryValue.allPaths.get.toSet
      }
      val inputs = inputFiles(filteredInputsValue).toSet
      cachedMerge(inputs)
      val filters = (filteredInputsValue flatMap (_.filter)).toSet
      val combinedFilter = if (filters.nonEmpty) Some(filters.mkString(",")) else None
      Seq(Filtered(mergeDirectoryValue, combinedFilter))
    }
  }

  lazy val proguardTask: Def.Initialize[Task[Seq[File]]] = Def.task {
    writeConfiguration(proguardConfiguration.value, proguardOptions.value)
    val proguardConfigurationValue = proguardConfiguration.value
    val javaOptionsInProguardValue = (proguard / javaOptions).value
    val managedClasspathValue = managedClasspath.value
    val streamsValue = streams.value
    val outputsValue = proguardOutputs.value
    val shouldRemoveTastyFiles = proguardRemoveTastyFiles.value
    val logger = streams.value.log
    val cachedProguard = FileFunction.cached(streams.value.cacheDirectory / "proguard", FilesInfo.hash) { _ =>
      outputsValue foreach IO.delete
      streamsValue.log.debug("Proguard configuration:")
      proguardOptions.value foreach (streamsValue.log.debug(_))
      runProguard(proguardConfigurationValue, javaOptionsInProguardValue, managedClasspathValue.files, streamsValue.log)
      if (shouldRemoveTastyFiles)
        outputsValue.foreach(removeTastyFiles(_, logger))
      outputsValue.toSet
    }
    val inputs = (proguardConfiguration.value +: inputFiles(proguardFilteredInputs.value)).toSet
    cachedProguard(inputs)
    proguardOutputs.value
  }

  def inputFiles(inputs: Seq[Filtered]): Seq[File] =
    inputs flatMap { i => if (i.file.isDirectory) i.file.allPaths.get else Seq(i.file) }

  def writeConfiguration(config: File, options: Seq[String]): Unit =
    IO.writeLines(config, options)

  def runProguard(config: File, javaOptions: Seq[String], classpath: Seq[File], log: Logger): Unit = {
    require(classpath.nonEmpty, "Proguard classpath cannot be empty!")
    val options = javaOptions ++ Seq("-cp", Path.makeString(classpath), "proguard.ProGuard", "-include", config.getAbsolutePath)
    log.info("Proguard command:")
    log.info("java " + options.mkString(" "))
    val exitCode = Process("java", options) ! log
    if (exitCode != 0) sys.error("Proguard failed with exit code [%s]" format exitCode)
  }

  private def removeTastyFiles(jar: File, logger: sbt.internal.util.ManagedLogger): Unit = {
    logger.info(s"removing .tasty files from $jar")
    val zipFs = FileSystems.newFileSystem(jar.toPath, null)
    zipFs.getRootDirectories.forEach { dir =>
      Files.walk(dir)
        .filter(_.toString.endsWith(".tasty"))
        .forEach(Files.delete)
    }
    zipFs.close()
  }
}


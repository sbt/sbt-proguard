package com.lightbend.sbt

import com.lightbend.sbt.proguard.Merge
import java.nio.file.{Files, FileSystems}
import sbt._
import sbt.Keys._
import sbt.internal.util.ManagedLogger
import scala.collection.JavaConverters._
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
    proguard / javaOptions := Seq("-Xmx256M"),
    autoImport.proguard := proguardTask.value
  )

  private def groupId(proguardVersionStr: String): String =
    "^(\\d+)\\.".r
      .findFirstMatchIn(proguardVersionStr)
      .map(_.group(1).toInt) match {
      case Some(v) => if (v > 6) "com.guardsquare" else "net.sf.proguard"
      case None => sys.error(s"Can't parse Proguard version: $proguardVersion")
    }

  def dependencies: Seq[Setting[_]] = Seq(
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
    val proguardOutputJar = (Proguard/proguardOutputs).value.head
    val cachedProguard = FileFunction.cached(streams.value.cacheDirectory / "proguard", FilesInfo.hash) { _ =>
      outputsValue foreach IO.delete
      streamsValue.log.debug("Proguard configuration:")
      proguardOptions.value foreach (streamsValue.log.debug(_))
      runProguard(proguardConfigurationValue, javaOptionsInProguardValue, managedClasspathValue.files, streamsValue.log)

      if (scalaBinaryVersion.value == "3") {
        streamsValue.log.info("This is a Scala 3 build - will now remove the TASTy files from the ProGuard outputs")
        val mappingsFile = findMappingsFileConfig(
          options = (Proguard/proguardOptions).value,
          baseDir = (Proguard/proguardDirectory).value
        ).getOrElse(throw new AssertionError(
          """mappings file not found in proguardOptions. Please configure it using e.g. `-printmapping mapings.txt`
            | - it must be configured for a Scala 3 build so we can remove the TASTy files for obfuscated classes""".stripMargin
        ))
        removeTastyFilesForObfuscatedClasses(
          mappingsFile,
          proguardOutputJar = proguardOutputJar,
          logger = streamsValue.log
        )
      }

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

  def removeTastyFilesForObfuscatedClasses(mappingsFile: File, proguardOutputJar: File, logger: ManagedLogger): Unit = {
    val obfuscatedClasses = findObfuscatedClasses(mappingsFile)

    if (obfuscatedClasses.nonEmpty) {
      logger.info(s"found ${obfuscatedClasses.size} classes that have been obfuscated; will now remove their TASTy files (bar some that are still required), since those contain even more information than the class files")
      // note: we must not delete the TASTy files for unobfuscated classes since that would break the REPL
      val tastyEntriesForObfuscatedClasses = obfuscatedClasses.map { className =>
        val zipEntry = "/" + className.replaceAll("\\.", "/") // `/` instead of `.`
        val tastyFileConvention = zipEntry.replaceFirst("\\$.*", "")
        s"$tastyFileConvention.tasty"
      }

      val deletedEntries = deleteFromJar(proguardOutputJar, tastyEntriesForObfuscatedClasses)
      logger.info(s"deleted ${deletedEntries.size} TASTy files from $proguardOutputJar")
      deletedEntries.foreach(println)
    }
  }

  def findMappingsFileConfig(options: Seq[String], baseDir: File): Option[File] = {
    options.find(_.startsWith("-printmapping")).flatMap { keyValue =>
      keyValue.split(" ") match {
        case Array(key, value) => Some(value)
        case _ =>
          None
      }
    }.map { value =>
      val mappingsFile = file(value)
      if (mappingsFile.isAbsolute) mappingsFile
      else baseDir / value
    }
  }

  def findObfuscatedClasses(mappingsFile: File): Set[String] = {
    // a typical mapping file entry looks like this:
    // `io.joern.x2cpg.passes.linking.filecompat.FileNameCompat -> io.joern.x2cpg.passes.a.a.a:`
    val mapping = "(.*) -> (.*):".r

    val classesThatHaveBeenObfuscated = for {
      line <- Files.lines(mappingsFile.toPath).iterator().asScala
      // the lines ending with `:` list the classname mappings:
      if line.endsWith(":")
      // extract the original and obfuscated name via regex matching:
      mapping(original, obfuscated) = line
      // if both sides are identical, this class didn't get obfuscated:
      if original != obfuscated
    } yield original

    classesThatHaveBeenObfuscated.toSet
  }

  /** Deletes all entries from a jar that is in the given set of entry paths.
    * Returns the Paths of the deleted entries. */
  def deleteFromJar(jar: File, toDelete: Set[String]): Seq[String] = {
    val zipFs = FileSystems.newFileSystem(jar.toPath, null: ClassLoader)

    val deletedEntries = for {
      zipRootDir <- zipFs.getRootDirectories.asScala
      entry <- Files.walk(zipRootDir).iterator.asScala
      if (toDelete.contains(entry.toString))
    } yield {
      Files.delete(entry)
      entry.toString
    }

    zipFs.close()
    deletedEntries.toSeq
  }
}

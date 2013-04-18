package com.typesafe.sbt

import sbt._
import sbt.Keys._
import com.typesafe.sbt.proguard.Merge

object SbtProguard extends Plugin {

  val Proguard = config("proguard").hide

  object ProguardKeys {
    import ProguardOptions.Filtered
    import Merge.Strategy

    val proguardVersion = SettingKey[String]("proguard-version")
    val proguardDirectory = SettingKey[File]("proguard-directory")
    val proguardConfiguration = SettingKey[File]("proguard-configuration")
    val binaryDeps = TaskKey[Seq[File]]("binaryDeps")
    val inputs = TaskKey[Seq[File]]("inputs")
    val libraries = TaskKey[Seq[File]]("libraries")
    val outputs = TaskKey[Seq[File]]("outputs")
    val defaultInputFilter = TaskKey[Option[String]]("default-input-filter")
    val inputFilter = TaskKey[File => Option[String]]("input-filter")
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

  lazy val proguardSettings: Seq[Setting[_]] = inConfig(Proguard)(ProguardSettings.default) ++ ProguardSettings.dependencies

  object ProguardSettings {
    import ProguardKeys._
    import ProguardOptions._

    def default: Seq[Setting[_]] = Seq(
      proguardVersion := "4.9",
      proguardDirectory <<= crossTarget / "proguard",
      proguardConfiguration <<= proguardDirectory / "configuration.pro",
      artifactPath <<= (proguardDirectory, artifactPath in packageBin in Compile) { (dir, path) => dir / path.getName },
      managedClasspath <<= (configuration, classpathTypes, update) map Classpaths.managedJars,
      binaryDeps <<= compile in Compile map { _.relations.allBinaryDeps.toSeq },
      inputs <<= dependencyClasspath in Compile map { _.files },
      libraries <<= (binaryDeps, inputs) map { (deps, in) => deps filterNot in.toSet },
      outputs <<= artifactPath map { Seq(_) },
      defaultInputFilter := Some("!META-INF/**"),
      inputFilter <<= defaultInputFilter map { default => { f => default } },
      filteredInputs <<= (inputs, inputFilter) map { (jars, filter) => jars map { jar => Filtered(jar, filter(jar)) } },
      filteredInputs <++= packageBin in Compile map noFilter,
      filteredLibraries <<= libraries map noFilter,
      filteredOutputs <<= outputs map noFilter,
      merge := false,
      mergeDirectory <<= proguardDirectory / "merged",
      mergeStrategies := Seq.empty,
      mergedInputs <<= mergeTask,
      options <<= (mergedInputs, filteredLibraries, filteredOutputs) map { (ins, libs, outs) =>
        jarOptions("-injars", ins) ++
        jarOptions("-libraryjars", libs) ++
        jarOptions("-outjars", outs)
      },
      javaOptions in proguard := Seq("-Xmx256M"),
      proguard <<= proguardTask
    )

    def dependencies: Seq[Setting[_]] = Seq(
      ivyConfigurations += Proguard,
      libraryDependencies <+= (proguardVersion in Proguard) { version =>
        "net.sf.proguard" % "proguard-base" % version % Proguard.name
      }
    )

    def mergeTask = (merge, filteredInputs, mergeDirectory, mergeStrategies, cacheDirectory, streams) map { (doMerge, filtered, dir, strategies, cache, s) =>
      if (doMerge) {
        val cachedMerge = FileFunction.cached(cache / "proguard-merge", FilesInfo.hash) { _ =>
          s.log.info("Merging inputs before proguard...")
          IO.delete(dir)
          val inputs = filtered map (_.file)
          Merge.merge(inputs, dir, strategies.reverse, s.log)
          dir.***.get.toSet
        }
        val inputs = inputFiles(filtered).toSet
        cachedMerge(inputs)
        val filters = (filtered flatMap (_.filter)).toSet
        val combinedFilter = if (filters.nonEmpty) Some(filters.mkString(",")) else None
        Seq(Filtered(dir, combinedFilter))
      } else filtered
    }

    def proguardTask = (proguardConfiguration, options, javaOptions in proguard, managedClasspath, filteredInputs, outputs, cacheDirectory, streams) map {
      (config, opts, javaOpts, cp, filtered, outputs, cache, s) => {
        writeConfiguration(config, opts)
        val cachedProguard = FileFunction.cached(cache / "proguard", FilesInfo.hash) { _ =>
          outputs foreach IO.delete
          s.log.debug("Proguard configuration:")
          opts foreach (s.log.debug(_))
          runProguard(config, javaOpts, cp.files, s.log)
          outputs.toSet
        }
        val inputs = (config +: inputFiles(filtered)).toSet
        cachedProguard(inputs)
        outputs
      }
    }

    def inputFiles(inputs: Seq[Filtered]): Seq[File] = {
      inputs flatMap { i => if (i.file.isDirectory) i.file.***.get else Seq(i.file) }
    }

    def writeConfiguration(config: File, options: Seq[String]): Unit = {
      val opts = options mkString "\n"
      IO.write(config, opts)
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

    def filtered(jars: Seq[File], filter: Option[String]): Seq[Filtered] = {
      jars map { jar => Filtered(jar, filter) }
    }

    def filterString(filter: Option[String]): String = {
      filter map { "(" + _ + ")" } getOrElse ""
    }

    def jarOptions(option: String, jars: Seq[Filtered]): Seq[String] = {
      jars map { jar => "%s \"%s\"%s" format (option, jar.file.getCanonicalPath, filterString(jar.filter)) }
    }

    def keepMain(name: String): String = {
      """-keep public class %s {
        |    public static void main(java.lang.String[]);
        |}""".stripMargin.format(name)
    }
  }
}

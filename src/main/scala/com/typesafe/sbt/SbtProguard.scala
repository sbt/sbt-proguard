package com.typesafe.sbt

import sbt._
import sbt.Keys._

object SbtProguard extends Plugin {

  case class FilteredJar(file: File, filter: Option[String])

  val Proguard = config("proguard").hide

  object ProguardKeys {
    val proguardVersion = SettingKey[String]("proguard-version")
    val proguardDirectory = SettingKey[File]("proguard-directory")
    val proguardConfiguration = SettingKey[File]("proguard-configuration")
    val binaryDeps = TaskKey[Seq[File]]("binaryDeps")
    val inJars = TaskKey[Seq[File]]("in-jars")
    val libraryJars = TaskKey[Seq[File]]("library-jars")
    val outJars = TaskKey[Seq[File]]("out-jars")
    val defaultInFilter = TaskKey[Option[String]]("default-in-filter")
    val filteredInJars = TaskKey[Seq[FilteredJar]]("filtered-in-jars")
    val filteredLibraryJars = TaskKey[Seq[FilteredJar]]("filtered-library-jars")
    val filteredOutJars = TaskKey[Seq[FilteredJar]]("filtered-out-jars")
    val options = TaskKey[Seq[String]]("options")
    val proguard = TaskKey[Seq[File]]("proguard")
  }

  import ProguardKeys._
  import ProguardOptions._

  lazy val proguardSettings: Seq[Setting[_]] = inConfig(Proguard)(defaultSettings) ++ dependencySettings

  def defaultSettings: Seq[Setting[_]] = Seq(
    proguardVersion := "4.9",
    proguardDirectory <<= crossTarget / "proguard",
    proguardConfiguration <<= proguardDirectory / "configuration.pro",
    artifactPath <<= (proguardDirectory, artifactPath in packageBin in Compile) { (dir, path) => dir / path.getName },
    managedClasspath <<= (configuration, classpathTypes, update) map Classpaths.managedJars,
    binaryDeps <<= compile in Compile map { _.relations.allBinaryDeps.toSeq },
    inJars <<= dependencyClasspath in Compile map { _.files },
    libraryJars <<= (binaryDeps, inJars) map { (deps, in) => deps filterNot in.toSet },
    outJars <<= artifactPath map { Seq(_) },
    defaultInFilter := Some("!META-INF/**"),
    filteredInJars <<= (inJars, defaultInFilter) map { (jars, filter) => addFilter(jars, filter) },
    filteredInJars <+= packageBin in Compile map { jar => FilteredJar(jar, None) },
    filteredLibraryJars <<= libraryJars map noFilter,
    filteredOutJars <<= outJars map noFilter,
    options <<= (filteredInJars, filteredLibraryJars, filteredOutJars) map { (in, library, out) =>
      jarOptions("-injars", in) ++
      jarOptions("-libraryjars", library) ++
      jarOptions("-outjars", out)
    },
    javaOptions in proguard := Seq("-Xmx256M"),
    proguard <<= proguardTask
  )

  def dependencySettings: Seq[Setting[_]] = Seq(
    ivyConfigurations += Proguard,
    libraryDependencies <+= (proguardVersion in Proguard) { version =>
      "net.sf.proguard" % "proguard-base" % version % Proguard.name
    }
  )

  def proguardTask = (proguardConfiguration, options, javaOptions in proguard, managedClasspath, filteredInJars, outJars, cacheDirectory, streams) map {
    (config, opts, javaOpts, cp, filteredInputs, outputs, cache, s) => {
      writeConfiguration(config, opts)
      val cached = FileFunction.cached(cache / "proguard", FilesInfo.hash) { _ =>
        outputs foreach IO.delete
        s.log.debug("Proguard configuration:")
        opts foreach (s.log.debug(_))
        runProguard(config, javaOpts, cp.files, s.log)
        outputs.toSet
      }
      val inputs = config +: (filteredInputs map (_.file))
      cached(inputs.toSet)
      outputs
    }
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

  object ProguardOptions {
    def noFilter(jars: Seq[File]): Seq[FilteredJar] = addFilter(jars, None)

    def addFilter(jars: Seq[File], filter: Option[String]): Seq[FilteredJar] = {
      jars map { jar => FilteredJar(jar, filter) }
    }

    def filterString(filter: Option[String]): String = {
      filter map { "(" + _ + ")" } getOrElse ""
    }

    def jarOptions(option: String, jars: Seq[FilteredJar]): Seq[String] = {
      jars map { jar => "%s \"%s\"%s" format (option, jar.file.getCanonicalPath, filterString(jar.filter)) }
    }

    def keepMain(name: String): String = {
      """-keep public class %s {
        |    public static void main(java.lang.String[]);
        |}""".stripMargin.format(name)
    }
  }
}

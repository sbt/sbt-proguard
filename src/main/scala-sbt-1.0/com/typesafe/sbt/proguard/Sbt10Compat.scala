package com.lightbend.sbt.proguard

import sbt.{Def, Task, _}
import sbt.internal.inc.Analysis
import sbt.Keys.compile
import xsbti.PathBasedFile

object Sbt10Compat {
  val getAllBinaryDeps: Def.Initialize[Task[Seq[java.io.File]]] = Def.task {
    (Compile / compile).value match {
      case analysis: Analysis =>
        analysis.relations.allLibraryDeps.collect { case vf: PathBasedFile =>
          vf.toPath.toFile
        }.toSeq
    }
  }

  val ClasspathUtilities = sbt.internal.inc.classpath.ClasspathUtilities

  val SbtIoPath = sbt.io.Path
}

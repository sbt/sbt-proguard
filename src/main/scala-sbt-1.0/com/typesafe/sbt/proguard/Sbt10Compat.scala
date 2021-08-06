package com.lightbend.sbt.proguard

import sbt.{Def, Task, _}
import sbt.internal.inc.Analysis
import sbt.Keys.compile
import xsbti._

import java.nio.file.{Files, StandardCopyOption}

object Sbt10Compat {
  val getAllBinaryDeps: Def.Initialize[Task[Seq[java.io.File]]] = Def.task {
    (Compile / compile).value match {
      case analysis: Analysis =>
        analysis.relations.allLibraryDeps.collect { case vf: VirtualFile =>
          val targetFile = new File(vf.name())
          Files.copy(vf.input(), targetFile.toPath, StandardCopyOption.REPLACE_EXISTING)
          targetFile
        }.toSeq
    }
  }

  val ClasspathUtilities = sbt.internal.inc.classpath.ClasspathUtilities

  val SbtIoPath = sbt.io.Path
}

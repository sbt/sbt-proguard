package com.typesafe.sbt.proguard

import sbt.{Def, Task, _}
import sbt.internal.inc.Analysis
import sbt.Keys.compile

object Sbt10Compat {
  val getAllBinaryDeps: Def.Initialize[Task[Seq[java.io.File]]] = Def.task {
    ((compile in Compile).value match {
      case analysis: Analysis =>
        analysis.relations.allLibraryDeps.toSeq
    })
  }

  val ClasspathUtilities = sbt.internal.inc.classpath.ClasspathUtilities

  val SbtIoPath = sbt.io.Path
}

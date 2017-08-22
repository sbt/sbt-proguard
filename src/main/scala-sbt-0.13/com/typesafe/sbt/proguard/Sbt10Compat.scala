package com.typesafe.sbt.proguard

import sbt.Keys.compile
import sbt.{File, _}

object Sbt10Compat {

  implicit class RichSbtFile(file: File) {
    def allPaths: PathFinder = file.***
  }

  implicit def toProcessLogger(logger: Logger): scala.sys.process.ProcessLogger =
    new scala.sys.process.ProcessLogger {
      override def out(s: => String): Unit = (logger: sbt.ProcessLogger).info(s)
      override def err(s: => String): Unit = (logger: sbt.ProcessLogger).error(s)
      override def buffer[T](f: => T): T = (logger: sbt.ProcessLogger).buffer(f)
    }

  val getAllBinaryDeps: Def.Initialize[Task[Seq[java.io.File]]] = Def.task {
    (compile in Compile).value.relations.allBinaryDeps.toSeq
  }

  val ClasspathUtilities = sbt.classpath.ClasspathUtilities

  object DummyPath

  val SbtIoPath = DummyPath
}

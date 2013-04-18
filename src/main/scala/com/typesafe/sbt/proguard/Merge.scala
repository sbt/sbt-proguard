package com.typesafe.sbt.proguard

import sbt._
import sbt.classpath.ClasspathUtilities
import java.io.File
import java.util.regex.Pattern

object Merge {
  object EntryPath {
    val pattern = Pattern.compile(if (File.separator == "\\") "\\\\" else File.separator)
  }

  case class EntryPath(path: String, isDirectory: Boolean) {
    val list = EntryPath.pattern.split(path).toList
    val name = if (list.isEmpty) "" else list.last
    val normalised = list.mkString("/") + (if (isDirectory) "/" else "")
    override def toString = normalised
    def file(base: File) = base / path
  }

  object Entry {
    def apply(path: String, file: File, source: File): Entry =
      Entry(EntryPath(path, file.isDirectory), file, source)
  }

  case class Entry(path: EntryPath, file: File, source: File)

  def entries(sources: Seq[File], tmp: File): Seq[Entry] = {
    sources flatMap { source =>
      val base = if (ClasspathUtilities.isArchive(source)) {
        val unzipped = tmp / source.getCanonicalPath
        IO.unzip(source, unzipped)
        unzipped
      } else source
      (base.*** --- base).get x relativeTo(base) map { p => Entry(p._2, p._1, source) }
    }
  }

  trait Strategy {
    def claims(path: EntryPath): Boolean
    def merge(path: EntryPath, entries: Seq[Entry], target: File, log: Logger): Unit
  }

  object Strategy {
    val deduplicate = new Strategy {
      def claims(path: EntryPath) = true
      def merge(path: EntryPath, entries: Seq[Entry], target: File, log: Logger): Unit = {
        Merge.deduplicate(path, entries, target, log)
      }
    }
  }

  def merge(sources: Seq[File], target: File, strategies: Seq[Strategy], log: Logger): Unit = {
    IO.withTemporaryDirectory { tmp =>
      var failed = false
      val groupedEntries = entries(sources, tmp) groupBy (_.path)
      for ((path, entries) <- groupedEntries) {
        val strategy = strategies find { _.claims(path) } getOrElse Strategy.deduplicate
        try {
          strategy.merge(path, entries, target, log)
        } catch {
          case e: Exception =>
            log.error(e.getMessage)
            failed = true
        }
      }
      if (failed) {
        sys.error("Failed to merge all sources. Merge strategies can be used to resolve this.")
        IO.delete(target)
      }
    }
  }

  def deduplicate(path: EntryPath, entries: Seq[Entry], target: File, log: Logger): Unit = {
    if (entries.size > 1) {
      if (path.isDirectory) {
        log.debug("Ignoring duplicate directory for '%s'" format path)
      } else {
        entries foreach { e => log.debug("Duplicate entry for '%s' from %s" format (e.path, e.source.name)) }
        sys.error("Duplicate entries for '%s'" format path)
      }
    } else {
      if (!path.isDirectory) copyFirst(entries, target)
    }
  }

  def copyFirst(entries: Seq[Entry], target: File): Unit = {
    for (entry <- entries.headOption) {
      IO.copyFile(entry.file, entry.path.file(target))
    }
  }
}

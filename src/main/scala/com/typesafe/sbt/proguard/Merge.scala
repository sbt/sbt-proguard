package com.typesafe.sbt.proguard

import sbt._
import sbt.io.Path._
import java.io.File
import java.util.regex.Pattern

import sbt.internal.inc.classpath.ClasspathUtilities

import scala.util.matching.Regex

object Merge {
  object EntryPath {
    val pattern = Pattern.compile(if (File.separator == "\\") "\\\\" else File.separator)

    def matches(s: String)(p: EntryPath) = p.matches(s)
    def matches(r: Regex)(p: EntryPath) = p.matches(r)
  }

  case class EntryPath(path: String, isDirectory: Boolean) {
    val list = EntryPath.pattern.split(path).toList
    val name = if (list.isEmpty) "" else list.last
    val normalised = list.mkString("/") + (if (isDirectory) "/" else "")

    def file(base: File) = base / path
    def matches(s: String) = s == normalised
    def matches(r: Regex) = r.findFirstIn(normalised).isDefined

    override def toString = normalised
  }

  object Entry {
    def apply(path: String, file: File, source: File): Entry =
      Entry(EntryPath(path, file.isDirectory), file, source)
  }

  case class Entry(path: EntryPath, file: File, source: File)

  def entries(sources: Seq[File], tmp: File): Seq[Entry] = {
    sources flatMap { source =>
      val base = if (ClasspathUtilities.isArchive(source)) {
        val path =
          if (source.getCanonicalPath.indexOf(":") > 0)
            source.getCanonicalPath.substring(source.getCanonicalPath.indexOf("\\") + 1,
              source.getCanonicalPath.length)
          else
            source.getCanonicalPath
        val unzipped = tmp / path
        IO.unzip(source, unzipped)
        unzipped
      } else source
      (base.allPaths --- base).get pair relativeTo(base) map { p => Entry(p._2, p._1, source) }
    }
  }

  trait Strategy {
    def claims(path: EntryPath): Boolean
    def merge(path: EntryPath, entries: Seq[Entry], target: File, log: Logger): Unit
  }

  object Strategy {
    type RunMerge = (EntryPath, Seq[Entry], File, Logger) => Unit

    val deduplicate: Strategy = create(_ => true, Merge.deduplicate)

    def matchingString(string: String, run: RunMerge): Strategy = create(EntryPath.matches(string), run)
    def matchingRegex(regex: Regex, run: RunMerge): Strategy = create(EntryPath.matches(regex), run)

    def create(claim: EntryPath => Boolean, run: RunMerge): Strategy = new Strategy {
      def claims(path: EntryPath): Boolean = claim(path)
      def merge(path: EntryPath, entries: Seq[Entry], target: File, log: Logger): Unit = run(path, entries, target, log)
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
        sys.error("Failed to merge all inputs. Merge strategies can be used to resolve conflicts.")
        IO.delete(target)
      }
    }
  }

  def deduplicate(path: EntryPath, entries: Seq[Entry], target: File, log: Logger): Unit = {
    if (entries.size > 1) {
      if (path.isDirectory) {
        log.debug("Ignoring duplicate directories at '%s'" format path)
        path.file(target).mkdirs
      } else {
        entries foreach { e => log.debug("Matching entry at '%s' from %s" format (e.path, e.source.name)) }
        val hashes = (entries map { _.file.hashString }).toSet
        if (hashes.size <= 1) {
          log.debug("Identical duplicates found at '%s'" format path)
          copyFirst(entries, target, Some(log))
        } else {
          sys.error("Multiple entries found at '%s'" format path)
        }
      }
    } else {
      if (path.isDirectory) path.file(target).mkdirs
      else copyFirst(entries, target)
    }
  }

  def copyFirst(entries: Seq[Entry], target: File, log: Option[Logger] = None): Unit = {
    entries.headOption foreach copyOne("first", target, log)
  }

  def copyLast(entries: Seq[Entry], target: File, log: Option[Logger] = None): Unit = {
    entries.lastOption foreach copyOne("last", target, log)
  }

  def copyOne(label: String, target: File, log: Option[Logger] = None)(entry: Entry): Unit = {
    log foreach { l => l.debug("Keeping %s entry at '%s' from %s" format (label, entry.path, entry.source.name)) }
    IO.copyFile(entry.file, entry.path.file(target))
  }

  def discard(path: EntryPath, entries: Seq[Entry], target: File, log: Logger): Unit = {
    entries foreach { e => log.debug("Discarding entry at '%s' from %s" format (e.path, e.source.name)) }
  }

  def first(path: EntryPath, entries: Seq[Entry], target: File, log: Logger): Unit = {
    if (path.isDirectory) path.file(target).mkdirs
    else copyFirst(entries, target, Some(log))
  }

  def last(path: EntryPath, entries: Seq[Entry], target: File, log: Logger): Unit = {
    if (path.isDirectory) path.file(target).mkdirs
    else copyLast(entries, target, Some(log))
  }

  def rename(path: EntryPath, entries: Seq[Entry], target: File, log: Logger): Unit = {
    if (path.isDirectory) sys.error("Rename of directory entry at '%s' is not supported" format path)
    for (entry <- entries) {
      val file = path.file(target)
      val renamed = new File(file.getParentFile, file.name + "-" + entry.source.name)
      log.debug("Renaming entry at '%s' to '%s'" format (path, renamed.name))
      IO.copyFile(entry.file, renamed)
    }
  }

  def append(path: EntryPath, entries: Seq[Entry], target: File, log: Logger): Unit = {
    if (path.isDirectory) sys.error("Append of directory entry at '%s' is not supported" format path)
    for (entry <- entries) {
      val file = path.file(target)
      log.debug("Appending entry at '%s' from '%s'" format (path, entry.source.name))
      IO.append(file, IO.readBytes(entry.file))
    }
  }
}

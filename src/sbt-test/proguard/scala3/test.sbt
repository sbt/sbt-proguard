import java.nio.file.{Files, FileSystems}
import collection.JavaConverters._
import scala.sys.process.Process

// for sbt scripted test:
TaskKey[Unit]("check") := {
  val expected = "test\n"
  val proguardResultJar = (Proguard / proguard).value.head
  val output = Process("java", Seq("-jar", proguardResultJar.absString)).!!
    .replaceAllLiterally("\r\n", "\n")
  if (output != expected) sys.error("Unexpected output:\n" + output)

  // older java releases (e.g. java 11) requires a classloader parameter, which may be null...
  // for later java release this isn't required any more
  val zipFs = FileSystems.newFileSystem(proguardResultJar.toPath, null: ClassLoader)
  val jarEntries = zipFs.getRootDirectories.asScala
    .flatMap(Files.walk(_).iterator.asScala)
    .toSeq
  zipFs.close()

  val obfuscateMeEntries = jarEntries.filter(_.toString.contains("ObfuscateMe"))
  assert(
    obfuscateMeEntries.isEmpty,
    s"""class `ObfuscateMe` should be obfuscated and not appear in the proguard output jar,
       |neither the class file nor the tasty file. However, we found the following: ${obfuscateMeEntries.mkString(",")}
       |""".stripMargin

  )
}


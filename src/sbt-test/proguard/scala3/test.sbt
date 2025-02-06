import java.nio.file.{Files, FileSystems}
import scala.jdk.CollectionConverters.*
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

  val actualTestClassEntries = jarEntries.map(_.toString).filter(_.startsWith("/Test")).toSet
  val expectedTestClassEntries = Set("/Test.tasty", "/Test.class")
  assert(
    actualTestClassEntries == expectedTestClassEntries, 
    s"""Test.class with has a `keep` rule and should be preserved, including the tasty file!
       |expected=$expectedTestClassEntries
       |  actual=$actualTestClassEntries
       |""".stripMargin
  )

  val obfuscateMeEntries = jarEntries.filter(_.toString.contains("ObfuscateMe"))
  assert(
    obfuscateMeEntries.isEmpty,
    s"""class `ObfuscateMe` should be obfuscated and not appear in the proguard output jar,
       |neither the class file nor the tasty file. However, we found the following: ${obfuscateMeEntries.mkString(",")}
       |""".stripMargin

  )
}


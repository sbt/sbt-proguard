import scala.sys.process.Process

// for sbt scripted test:
TaskKey[Unit]("check") := {
  val expected = "test\n"
  val output = Process("java", Seq("-classpath", (Proguard / proguard).value.absString, "Test")).!!
    .replaceAllLiterally("\r\n", "\n")
  if (output != expected) sys.error("Unexpected output:\n" + output)
}

import scala.sys.process.Process

// for sbt scripted test:
TaskKey[Unit]("check") := {
  val cp = (proguard in Proguard).value
  val expected = "test\n"
  val output = Process("java", Seq("-classpath", cp.absString, "Test")).!!
    .replaceAllLiterally("\r\n", "\n")
  if (output != expected) sys.error("Unexpected output:\n" + output)
}

import scala.sys.process.Process

// for sbt scripted test:
TaskKey[Unit]("check") := {
  val expected = "test\n"
  val output = Process("java", Seq("-jar", (Proguard / proguard).value.absString)).!!
    .replaceAllLiterally("\r\n", "\n")
  if (output != expected) sys.error("Unexpected output:\n" + output)
}

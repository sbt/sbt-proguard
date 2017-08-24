import scala.sys.process.Process

// for sbt scripted test:
TaskKey[Unit]("check") := {
  val cp = (proguard in Proguard).value
  val expected = "hello world\n"
  val a = Process("java", Seq("-classpath", cp.absString, "A"))
  val b = Process("java", Seq("-classpath", cp.absString, "B"))
  a.run()
  Thread.sleep(1000)
  val output = b.!!
    .replaceAllLiterally("\r\n", "\n")
  if (output != expected) sys.error("Unexpected output:\n" + output)
}

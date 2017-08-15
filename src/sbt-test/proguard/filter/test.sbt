// for sbt scripted test:
TaskKey[Unit]("check") := {
  val expected = "test\n"
  val output = Process("java", Seq("-classpath", (ProguardKeys.proguard in Proguard).value.absString, "Test")).!!
  if (output != expected) sys.error("Unexpected output:\n" + output)
}

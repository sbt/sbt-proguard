// for sbt scripted test:
TaskKey[Unit]("check") := {
  val expected = "test\n"
  val output = Process("java", Seq("-jar", (ProguardKeys.proguard in Proguard).value.absString)).!!
  if (output != expected) sys.error("Unexpected output:\n" + output)
}

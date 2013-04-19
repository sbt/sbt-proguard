// for sbt scripted test:
TaskKey[Unit]("check") <<= (ProguardKeys.proguard in Proguard) map { cp =>
  val expected = "test\n"
  val output = Process("java", Seq("-classpath", cp.absString, "Test")).!!
  if (output != expected) sys.error("Unexpected output:\n" + output)
}

// for sbt scripted test:
TaskKey[Unit]("check") <<= (ProguardKeys.proguard in Proguard) map { jar =>
  val expected = "test\n"
  val output = Process("java", Seq("-jar", jar.absString)).!!
  if (output != expected) sys.error("Unexpected output:\n" + output)
}

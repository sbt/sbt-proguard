// for sbt scripted test:
TaskKey[Unit]("check") <<= (ProguardKeys.proguard in Proguard) map { cp =>
  val expected = "hello world\n"
  val a = Process("java", Seq("-classpath", cp.absString, "A"))
  val b = Process("java", Seq("-classpath", cp.absString, "B"))
  a.run()
  Thread.sleep(1000)
  val output = b.!!
  if (output != expected) sys.error("Unexpected output:\n" + output)
}

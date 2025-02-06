object Test {
  def main(args: Array[String]) = {
    println(ObfuscateMe.foo)
  }
}


object ObfuscateMe {
  def foo: String = "test"
}

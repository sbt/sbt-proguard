import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import scala.concurrent.Await
import scala.concurrent.duration._

object A extends App {
  val system = ActorSystem("A", ConfigFactory.load("A"))
  val actor = system.actorOf(Props[TestActor], "test")
}

object B extends App {
  val system = ActorSystem("B", ConfigFactory.load("B"))
  val actor = system.actorFor("akka://A@127.0.0.1:7771/user/test")

  implicit val timeout = Timeout(1.second)
  val result = Await.result(actor ? "world", timeout.duration)
  println(result)

  actor ! "shutdown"
  system.shutdown()
}

class TestActor extends Actor {
  def receive = {
    case "shutdown" => context.system.shutdown()
    case message    => sender ! s"hello $message"
  }
}

import DummyProtocol._
import akka.actor._
import akka.util.Timeout
import scala.concurrent._
import scala.concurrent.duration._
import com.github.ktonga.akka.contextual.actor._
import com.github.ktonga.akka.contextual.pattern.{ask, pipe}
import ExecutionContext.Implicits.global


object DummyProtocol {

  case class Tell(param: String)

  case class Ask(param: String)

  case class Forward(param: String)

  case class Pipe(param: String)

  case class AskResponse(response: String)

  case class ForwardResponse(response: String)

  case object ForwardBehavior

  case object TellBehavior

}

class ActorA(val actorB: ActorRef) extends Actor with TracingActor {

  implicit val _: Timeout = 5.seconds

  def receive: Receive = {
    case Tell(str) => doTell(str)
    case Ask(question) => (actorB ?+ Ask(s"do you know $question?")) |+ sender
    case fwr: ForwardResponse => log.info("Thanks! {}", fwr)
    case ForwardBehavior => context.become(forwardBehavior, false)
  }

  val forwardBehavior: Receive = {
    case fw: Forward => actorB !+ fw
    case fwr: ForwardResponse => log.info("Thanks! {}", fwr)
    case TellBehavior => context.unbecome()
  }


  def doTell(str: String) = {
    log.info("A received: {}", str)
    actorB !+ str.length
  }

  override def unhandled(message: Any): Unit = log.error("Unhandled But Controlled Message: {}", message)
}

class ActorB(val actorC: ActorRef) extends Actor with TracingActor {
  import MessageContext._

  def receive: Receive = {
    case int: Int => useInt(int)
    case Ask(question) => {
      val resp: Msg[AskResponse] = AskResponse(s"I don't know $question")
      val theSender = sender
      context.system.scheduler.scheduleOnce(2 seconds) {
        theSender !+ resp
      }
    }
    case fw: Forward => actorC >+ fw
  }

  def useInt(int: Int) = {
    log.info("B received length: {}", int)
  }
}

class ActorC extends Actor with TracingActor {

  def receive: Receive = {
    case Forward(str) => sender !+ ForwardResponse(s"They forwarded me this: $str")
  }

}

object Sample extends App {

  import Implicits._
  import scala.util.Random
  import MessageContext._

  val system = ActorSystem("tracing-system")

  val actorC = system.actorOf(Props[ActorC], "actor-c")
  val actorB = system.actorOf(Props(new ActorB(actorC)), "actor-b")
  val actorA = system.actorOf(Props(new ActorA(actorB)), "actor-a")

  implicit val _: Timeout = 5.seconds

  implicit val ctx = Some(MsgCtx(Map("requestId" -> s"implicit-${Random.nextInt(1000)}")))

  actorA !+ Tell("With Implicit Context")
  actorA ! Msg(Tell("Context Specified Explicitly"), Some(MsgCtx(Map("requestId" -> s"explicit-${Random.nextInt(1000)}"))))
  actorA !+ Forward("To dead-letters")
  actorA !+ ForwardBehavior
  actorA !+ Forward("To Actor C")
  actorA !+ Ask("To dead-letters")
  actorA !+ TellBehavior
  val resp = actorA ?+ Ask("what is the meaning of life")

  system.log.info(Await.result(resp, Duration.Inf).toString)
  system.shutdown()
}

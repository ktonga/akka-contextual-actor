package com.ktonga.akka.contextual.actor

import DummyProtocol._
import akka.actor._
import akka.util.Timeout
import scala.concurrent._
import scala.concurrent.duration._
import com.ktonga.akka.contextual.actor.pattern.{ask, pipe}
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

class ActorA(val actorB: ActorRef) extends ContextualActor {

  implicit val _: Timeout = 5.seconds

  def receiveW: Receive = {
    case Tell(str) => doTell(str)
    case Ask(question) => (actorB ? Ask(s"do you know $question?")) pipeTo sender
    case fwr: ForwardResponse => info("Thanks! " + fwr)
    case ForwardBehavior => become(forwardBehavior, false)
  }

  val forwardBehavior: Receive = {
    case fw: Forward => actorB !! fw
    case fwr: ForwardResponse => info("Thanks! " + fwr)
    case TellBehavior => unbecome()
  }


  def doTell(str: String) = {
    info(s"A received: $str")
    actorB !! str.length
  }

//  override def unhandled(message: Any): Unit = error("Unhandled But Controlled Message: " + message)
}

class ActorB(val actorC: ActorRef) extends ContextualActor {
  import MessageContext._

  def receiveW: Receive = {
    case int: Int => useInt(int)
    case Ask(question) => {
      val resp: Msg[AskResponse] = AskResponse(s"I don't know $question")
      val theSender = sender
      context.system.scheduler.scheduleOnce(2 seconds) {
        theSender !! resp
      }
    }
    case fw: Forward => actorC forwardW fw
  }

  def useInt(int: Int) = {
    info("B received length: %d", int)
  }
}

class ActorC extends ContextualActor {

  def receiveW: Receive = {
    case Forward(str) => sender !! ForwardResponse(s"They forwarded me this: $str")
  }

}

object Sample extends App {

  import Implicits._
  import scala.util.Random
  import MessageContext._


  val system = ActorSystem("stacked-traits")

  val actorC = system.actorOf(Props[ActorC], "actor-c")
  val actorB = system.actorOf(Props(new ActorB(actorC)), "actor-b")
  val actorA = system.actorOf(Props(new ActorA(actorB)), "actor-a")

  implicit val _: Timeout = 5.seconds

  implicit val ctx = Some(MsgCtx("implicit-" + Random.nextInt(1000)))

  actorA !! Tell("With Implicit Context")
  actorA ! Msg(Tell("Context Specified Explicitly"), Some(MsgCtx("explicit-" + Random.nextInt(1000))))
  actorA !! Forward("To dead-letters")
  actorA !! ForwardBehavior
  actorA !! Forward("To Actor C")
  actorA !! Ask("To dead-letters")
  actorA !! TellBehavior
  val resp = actorA ? Ask("what is the meaning of life")

  println("************ " + Await.result(resp, Duration.Inf))
  system.shutdown()
}

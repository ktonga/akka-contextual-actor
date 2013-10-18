package com.ktonga.akka.contextual.actor.pattern

import com.ktonga.akka.contextual.actor.MessageContext._
import com.ktonga.akka.contextual.actor.Implicits
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import akka.actor._
import akka.util.Timeout
import akka.pattern.{ask => akkaAsk}


trait AskSupport {
  implicit def ask(actorRef: ActorRef): ContextualAskableActorRef = new ContextualAskableActorRef(actorRef)
}

final class ContextualAskableActorRef(val actorRef: ActorRef) extends AnyVal {
  def ?(msg: Msg[_])(implicit timeout: Timeout): Future[Any] = akkaAsk(actorRef, msg)
}

trait PipeToSupport {
  implicit def pipe[T](future: Future[T])(implicit executionContext: ExecutionContext): ContextualPimpedFuture[T] = new ContextualPimpedFuture(future)
}

final class ContextualPimpedFuture[T](val future: Future[T])(implicit executionContext: ExecutionContext) extends Implicits {
  def pipeTo(recipient: ActorRef)(implicit ctx: Option[MsgCtx], sender: ActorRef = Actor.noSender): Future[T] = {
    future onComplete {
      case Success(Msg(r, _)) ⇒ recipient !! r
      case Success(r) ⇒ recipient !! r
      case Failure(f) ⇒ recipient !! Status.Failure(f)
    }
    future
  }
}



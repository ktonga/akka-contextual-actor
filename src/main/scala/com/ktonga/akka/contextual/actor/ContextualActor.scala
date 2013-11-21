package com.ktonga.akka.contextual.actor

import akka.actor._

object MessageContext {

  case class MsgCtx(attr: String)

  case class Msg[M](msg: M, ctx: Option[MsgCtx] = None)

}

final class ContextualActorRef(val ref: ActorRef) extends AnyVal {
  import MessageContext._

  def tellW(msg: Msg[_], sender: ActorRef): Unit = ref.tell(msg, sender)

  def !!(msg: Msg[_])(implicit sender: ActorRef = Actor.noSender): Unit = ref.!(msg)

  def forwardW(msg: Msg[_])(implicit context: ActorContext): Unit = ref.forward(msg)
}

trait Implicits {

  import MessageContext._

  implicit def any2Msg[T](m: T)(implicit ctx: Option[MsgCtx]): Msg[T] = Msg(m, ctx)

  implicit def actorRef2ContextualActorRef(ref: ActorRef): ContextualActorRef = new ContextualActorRef(ref)
}

object Implicits extends Implicits

trait MessageContext {

  import MessageContext._

  implicit var msgCtx: Option[MsgCtx] = None

  def doWithContext(message: Any)(f: Any => Unit): Unit = {
    val (ctx, msg) = message match {
      case wrapper: Msg[_] => (wrapper.ctx, wrapper.msg)
      case other => (None, other)
    }
    msgCtx = ctx
    f(msg)
    msgCtx = None
  }

}

trait Logging extends DiagnosticActorLogging {
  this: MessageContext =>

  import scala.compat.Platform
  import akka.event

  override def mdc(currentMessage: Any): event.Logging.MDC = {
    msgCtx.map(c => Map("requestId" -> c.attr)).getOrElse(Map())
  }

  def runWithTime(f: => Unit)(g: Long => Unit) = {
    val start = Platform.currentTime
    f
    val spentTime = Platform.currentTime - start
    g(spentTime)
  }

  def debugWithTime(template: String, args: Any*)(f: => Unit) = {
    runWithTime(f) {
      spentTime =>
        log.debug(template + " - Spent Time: {}ms", (args :+ spentTime).toArray)
    }
  }

  def infoWithTime(template: String, args: Any*)(f: => Unit) = {
    runWithTime(f) {
      spentTime =>
        log.info(template + " - Spent Time: {}ms", (args :+ spentTime).toArray)
    }
  }

}

trait BaseActor extends Logging with akka.ContextualActor with Implicits
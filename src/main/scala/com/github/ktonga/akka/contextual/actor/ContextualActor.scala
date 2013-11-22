package com.github.ktonga.akka.contextual.actor

import akka.actor._

object MessageContext {

  case class MsgCtx(attributes: Map[String, Any])

  case class Msg[M](msg: M, ctx: Option[MsgCtx] = None)

}

final class ContextualActorRef(val ref: ActorRef) extends AnyVal {
  import MessageContext._

  def tellWithCtx(msg: Msg[_], sender: ActorRef): Unit = ref.tell(msg, sender)

  def !+(msg: Msg[_])(implicit sender: ActorRef = Actor.noSender): Unit = ref.!(msg)

  def forwardWithContext(msg: Msg[_])(implicit context: ActorContext): Unit = ref.forward(msg)

  def >+(msg: Msg[_])(implicit context: ActorContext): Unit = ref.forward(msg)
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
  import com.github.ktonga.akka.contextual.actor.MessageContext.MsgCtx

  /**
   * Hook to build the MDC map from the current message context.
   * It returns all the context attributes by default, but can be
   * overriden to filter out some values o add some derived values.
   *
   * @param ctx the current message context to extract MDC values from
   * @return the map to be used as MDC values
   */
  def ctx2mdc(ctx: MsgCtx): akka.event.Logging.MDC = ctx.attributes

  override def mdc(currentMessage: Any): event.Logging.MDC = {
    msgCtx.map(ctx2mdc).getOrElse(Map())
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

trait TracingActor extends Logging with akka.ContextualActor with Implicits
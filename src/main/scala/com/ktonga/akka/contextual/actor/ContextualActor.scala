package com.ktonga.akka.contextual.actor

import akka.actor._
import com.ktonga.akka.contextual.logging.BusLogging

trait WrappedReceive {
  this: Actor =>

  def receiveW: Receive
  def wrapReceive(receive: Receive): Receive

  final def receive: Receive = wrapReceive(receiveW)

  def become(behavior: Receive, discardOld: Boolean = true): Unit = {
    context.become(wrapReceive(behavior), discardOld)
  }

  def unbecome(): Unit = {
    context.unbecome()
  }
}

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

trait MessageContext extends WrappedReceive {
  this: Actor =>

  import MessageContext._

  implicit var msgCtx: Option[MsgCtx] = None

  def wrapReceive(receive: Receive): Receive = new Receive {
    def apply(a: Any): Unit = {
      val (ctx, msg) = a match {
        case wrapper: Msg[_] => (wrapper.ctx, wrapper.msg)
        case other => (None, other)
      }
      msgCtx = ctx
      receive(msg)
      msgCtx = None
    }
    def isDefinedAt(a: Any): Boolean = a match {
      case wrapper: Msg[_] => receive.isDefinedAt(wrapper.msg)
      case other => receive.isDefinedAt(other)
    }
  }

}

trait MessageLogging extends WrappedReceive {
  this: Actor with Logging =>

  abstract override def wrapReceive(receive: Receive): Receive = {
    super.wrapReceive(new Receive {
      def apply(a: Any): Unit = infoWithTime("Message: %s", a) {receive(a)}
      def isDefinedAt(a: Any): Boolean = receive.isDefinedAt(a)
    })
  }
}

object Logging {

  import akka.event.Logging.LogLevel

  case class Fmt(template: String, args: Any*) {
    override def toString = template format (args: _*)
  }

  case class Log(level: LogLevel, cause: Option[Throwable], fmt: Fmt)

  val template = "{}{}"
}

trait Logging {
  this: Actor with MessageContext =>

  import MessageContext._
  import Logging._
  import akka.event.{Logging => akkaLogging}
  import scala.compat.Platform
  import com.ktonga.akka.contextual.logging.MsgWithMDC

  val log = BusLogging(context.system, this)

  def formatContext(ctx: Option[MsgCtx]): Fmt = Fmt("[%s] ", ctx.map(_.attr).getOrElse("empty"))

  def logWithContext(logObj: Log)(implicit ctx: Option[MsgCtx]) = {
    val mdc = ctx.map(c => Map("requestId" -> c.attr)).getOrElse(Map())
    logObj match {
      case Log(akkaLogging.ErrorLevel, Some(cause), fmt) => log.error(cause, MsgWithMDC(fmt, mdc))
      case Log(level, _, fmt) => log.log(level, MsgWithMDC(fmt, mdc))
    }
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
        debug(template + " - Spent Time: %sms", args :+ spentTime: _*)
    }
  }

  def infoWithTime(template: String, args: Any*)(f: => Unit) = {
    runWithTime(f) {
      spentTime =>
        info(template + " - Spent Time: %sms", args :+ spentTime: _*)
    }
  }

  def debug(template: String, args: Any*) = {
    logWithContext(Log(akkaLogging.DebugLevel, None, Fmt(template, args: _*)))
  }

  def info(template: String, args: Any*) = {
    logWithContext(Log(akkaLogging.InfoLevel, None, Fmt(template, args: _*)))
  }

  def warn(template: String, args: Any*) = {
    logWithContext(Log(akkaLogging.WarningLevel, None, Fmt(template, args: _*)))
  }

  def error(template: String, args: Any*) = {
    logWithContext(Log(akkaLogging.ErrorLevel, None, Fmt(template, args: _*)))
  }

  def error(cause: Throwable, template: String, args: Any*) = {
    logWithContext(Log(akkaLogging.ErrorLevel, Some(cause), Fmt(template, args: _*)))
  }

}

trait ContextualActor extends Actor with MessageContext with Implicits with Logging with MessageLogging
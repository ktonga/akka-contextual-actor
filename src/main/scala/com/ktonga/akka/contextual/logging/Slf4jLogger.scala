package com.ktonga.akka.contextual.logging

/**
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */

import akka.actor._
import akka.event.slf4j.Logger
import akka.event.slf4j.SLF4JLogging
import akka.event.Logging._
import org.slf4j.MDC
import akka.util.Helpers


case class MsgWithMDC(msg: Any, mdc: Map[String, Any])

/**
 * SLF4J logger.
 *
 * The thread in which the logging was performed is captured in
 * Mapped Diagnostic Context (MDC) with attribute name "sourceThread".
 */
class Slf4jLogger extends Actor with SLF4JLogging {

  val mdcThreadAttributeName = "sourceThread"
  val mdcAkkaSourceAttributeName = "akkaSource"
  val mdcAkkaTimestamp = "akkaTimestamp"

  def receive = {

    case event @ Error(cause, logSource, logClass, message) ⇒
      withMdc(logSource, event) { msg =>
        cause match {
          case Error.NoCause | null ⇒ Logger(logClass, logSource).error(if (msg != null) msg.toString else null)
          case theCause                ⇒ Logger(logClass, logSource).error(if (msg != null) msg.toString else theCause.getLocalizedMessage, cause)
        }
      }

    case event @ Warning(logSource, logClass, message) ⇒
      withMdc(logSource, event) { msg => Logger(logClass, logSource).warn("{}", msg.asInstanceOf[AnyRef]) }

    case event @ Info(logSource, logClass, message) ⇒
      withMdc(logSource, event) { msg => Logger(logClass, logSource).info("{}", msg.asInstanceOf[AnyRef]) }

    case event @ Debug(logSource, logClass, message) ⇒
      withMdc(logSource, event) { msg => Logger(logClass, logSource).debug("{}", msg.asInstanceOf[AnyRef]) }

    case InitializeLogger(_) ⇒
      log.info("Slf4jLogger started")
      sender ! LoggerInitialized
  }

  @inline
  final def withMdc(logSource: String, logEvent: LogEvent)(logStatement: Any ⇒ Unit) {
    val (msg, mdc) = logEvent.message match {
      case MsgWithMDC(_msg, _mdc) => (_msg, _mdc)
      case m => (m, Map[String, Any]())
    }
    MDC.put(mdcAkkaSourceAttributeName, logSource)
    MDC.put(mdcThreadAttributeName, logEvent.thread.getName)
    MDC.put(mdcAkkaTimestamp, formatTimestamp(logEvent.timestamp))
    mdc foreach {case (k, v) => MDC.put(k, v.toString)}
    try logStatement(msg) finally {
      MDC.remove(mdcAkkaSourceAttributeName)
      MDC.remove(mdcThreadAttributeName)
      MDC.remove(mdcAkkaTimestamp)
      mdc.keys.foreach(k => MDC.remove(k))
    }
  }

  /**
   * Override this method to provide a differently formatted timestamp
   * @param timestamp a "currentTimeMillis"-obtained timestamp
   * @return the given timestamp as a UTC String
   */
  protected def formatTimestamp(timestamp: Long): String =
    Helpers.currentTimeMillisToUTCString(timestamp)
}


package com.ktonga.akka.contextual.logging

import akka.event.Logging._
import akka.event.{LogSource, LoggingBus}
import akka.actor.{Actor, ActorSystem}

object BusLogging {

  def apply(system: ActorSystem, a: Actor) = {
    val logger = a.self.path.elements.mkString(".")
    val (str, clazz) = LogSource.fromAnyRef(logger)
    new BusLogging(system.eventStream, str, clazz)
  }
}

class BusLogging(val bus: LoggingBus, val logSource: String, val logClass: Class[_]) {

  def error(cause: Throwable, message: Any): Unit = bus.publish(Error(cause, logSource, logClass, message))
  def log(level: LogLevel, message: Any): Unit = level match {
    case ErrorLevel => bus.publish(Error(logSource, logClass, message))
    case WarningLevel => bus.publish(Warning(logSource, logClass, message))
    case InfoLevel => bus.publish(Info(logSource, logClass, message))
    case DebugLevel => bus.publish(Debug(logSource, logClass, message))
  }

}

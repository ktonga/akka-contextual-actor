package akka

import akka.actor.Actor
import com.github.ktonga.akka.contextual.actor._

trait ContextualActor extends Actor with MessageContext {
  protected[akka] override def aroundReceive(receive: Actor.Receive, msg: Any): Unit = {
    doWithContext(msg) {m => super.aroundReceive(receive, m)}
  }
}
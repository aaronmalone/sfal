package edu.osu.sfal

import akka.actor.{Inbox, ActorSystem, ActorRef}
import edu.osu.sfal.messages.GetLastMessage
import scala.concurrent.duration._

package object actors {
  /**
   * Get the last message received by an actor that extends LastMessageReceivedActor.
   * This only works with actors extending LastMessageReceivedActor, but there is no
   * way to enforce that at compile time.
   */
  def getLastMessageReceivedByActor(actorRef: ActorRef, system: ActorSystem): Any = {
    val inbox = Inbox.create(system)
    actorRef.tell(GetLastMessage.INSTANCE, inbox.getRef())
    inbox.receive(50 milliseconds)
  }
}

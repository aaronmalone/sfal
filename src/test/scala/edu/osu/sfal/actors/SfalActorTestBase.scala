package edu.osu.sfal.actors

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{WordSpecLike, BeforeAndAfterAll}

class SfalActorTestBase(val actorSystem: ActorSystem)
  extends TestKit(actorSystem)
  with ImplicitSender
  with WordSpecLike
  with BeforeAndAfterAll {

  def this() = this(ActorSystem("sfalActorTest"))

  override protected def afterAll(): Unit = {
    actorSystem.shutdown()
  }
}

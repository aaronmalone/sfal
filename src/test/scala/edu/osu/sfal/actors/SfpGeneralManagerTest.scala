package edu.osu.sfal.actors

import org.scalatest.WordSpec
import edu.osu.sfal.messages.SfApplicationRequest
import akka.testkit.TestActorRef
import akka.actor.{ActorRef, Props, ActorSystem}
import edu.osu.sfal.util.{SfpName, SimulationFunctionName}
import edu.osu.sfal.messages.sfp.NewSfp
import java.util.HashMap
import com.google.common.collect.Sets
import edu.osu.sfal.actors.creators.SfpPoolManagerCreatorFactory

class SfpGeneralManagerTest extends WordSpec {

  val system = ActorSystem.create("test")

  class SfpGeneralManagerTestFixture extends ActorTestFixture {
    private val sfpPoolManagerCreatorFactory = new SfpPoolManagerCreatorFactory(mockLapisApi)
    private val props = Props(classOf[SfpGeneralManager], sfpPoolManagerCreatorFactory)
    val testActorRef = TestActorRef.create[SfpGeneralManager](system, props)
    val sfpGeneralManager = testActorRef.underlyingActor
    assert(sfpGeneralManager.sfpPoolMap.isEmpty)
    val newSfp = new NewSfp(simulationFunctionName, sfpName)
  }

  "The SfpGeneralManager" when {
    "it receives a " + classOf[NewSfp].getName + " message," should {
      "should create a new SfpPoolManager if none exists for the simulation function " +
          "and dispatch the NewSfp message to the SfpPoolManager" in {
        val fxt = newFixtureWithSfpPool()
        val sfpPoolManagerActor = getSfpPoolManagerActorRef(fxt)
        assert(sfpPoolManagerActor != null)
        val lastMsgReceived = getLastMessageReceivedByActor(sfpPoolManagerActor, system)
        assert(fxt.newSfp === lastMsgReceived)
      }
    }
    "it receives a " + classOf[NewSfp].getName + " message for an existing SFP pool," should {
      "dispatch the message to the corresponding SfpPoolManager" in {
        val fxt = newFixtureWithSfpPool()
        val secondNewSfpMessage = new NewSfp(fxt.simulationFunctionName, new SfpName("secondSFP"))
        fxt.testActorRef ! secondNewSfpMessage
        assert(fxt.sfpGeneralManager.sfpPoolMap.size() === 1) //no new SfpPoolManager actors added
        val sfpPoolManager = getSfpPoolManagerActorRef(fxt)
        assert(secondNewSfpMessage === getLastMessageReceivedByActor(sfpPoolManager, system))
      }
    }
    "it receives a " + classOf[SfApplicationRequest].getName + " message, " should {
      "forward to the appropriate SfpPoolManager" in {
        val fxt = newFixtureWithSfpPool()
        val request = new SfApplicationRequest(fxt.simulationFunctionName, 0, new HashMap(), Sets.newHashSet())
        fxt.testActorRef ! request
        val sfpPoolManager = getSfpPoolManagerActorRef(fxt)
        assert(request === getLastMessageReceivedByActor(sfpPoolManager, system))
      }
    }
  }

  def getSfpPoolManagerActorRef(fxt: SfpGeneralManagerTestFixture): ActorRef = {
    fxt.sfpGeneralManager.sfpPoolMap.get(fxt.simulationFunctionName)
  }

  def newFixtureWithSfpPool():SfpGeneralManagerTestFixture = {
    val fxt = new SfpGeneralManagerTestFixture()
    fxt.testActorRef ! fxt.newSfp
    fxt
  }
}

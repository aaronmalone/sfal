package edu.osu.sfal.actors

import org.scalatest.WordSpec
import edu.osu.sfal.messages.SfApplicationRequest
import akka.testkit.TestActorRef
import akka.actor.{ActorRef, Props, ActorSystem}
import edu.osu.sfal.util.{SfpName, SimulationFunctionName}
import org.apache.commons.lang3.RandomStringUtils
import edu.osu.sfal.messages.sfp.NewSfp
import java.util.HashMap
import com.google.common.collect.Sets
import akka.japi.Creator
import edu.osu.sfal.actors.creators.{SfpPoolManagerCreatorFactory, SfpActorCreatorFactory}

class SfpGeneralManagerTest extends WordSpec {

  def randomString() = RandomStringUtils.randomAlphanumeric(10)

  val system = ActorSystem.create("test")



  class Fixture() {
    val simulationFunctionName = new SimulationFunctionName(randomString())
    private val sfpActorCreatorFactory = new SfpActorCreatorFactory(null, simulationFunctionName)
    private val sfpPoolManagerCreatorFactory = new SfpPoolManagerCreatorFactory(null)
    private val props = Props(classOf[SfpGeneralManager], sfpPoolManagerCreatorFactory)
    val testActorRef = TestActorRef.create[SfpGeneralManager](system, props)
    val sfpGeneralManager = testActorRef.underlyingActor
    assert(sfpGeneralManager.sfpPoolMap.isEmpty)
    val sfpName = new SfpName(randomString())
    val newSfp = new NewSfp(simulationFunctionName, sfpName)
  }

  "The SfpGeneralManager" when {
    "it receives a " + classOf[NewSfp].getName + " message," should {
      "should create a new SfpPoolManager is none exists for the simulation function " +
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
        val fxt = new Fixture()
        fxt.testActorRef ! fxt.newSfp //register new SFP to create pool
        val secondNewSfpMessage = new NewSfp(fxt.simulationFunctionName, new SfpName(randomString()+"second"))
        fxt.testActorRef ! secondNewSfpMessage
        assert(fxt.sfpGeneralManager.sfpPoolMap.size() === 1) //no new SfpPoolManager actors added
        val sfpPoolManager = getSfpPoolManagerActorRef(fxt)
        assert(secondNewSfpMessage === getLastMessageReceivedByActor(sfpPoolManager, system))
      }
    }
    "it receives a " + classOf[SfApplicationRequest].getName() + " message, " should {
      "forward to the appropriate SfpPoolManager" in {
        val fxt = newFixtureWithSfpPool()
        val request = new SfApplicationRequest(fxt.simulationFunctionName, 0, new HashMap(), Sets.newHashSet())
        fxt.testActorRef ! request
        val sfpPoolManager = getSfpPoolManagerActorRef(fxt)
        assert(request === getLastMessageReceivedByActor(sfpPoolManager, system))
      }
    }
  }

  def getSfpPoolManagerActorRef(fxt: Fixture): ActorRef = {
    fxt.sfpGeneralManager.sfpPoolMap.get(fxt.simulationFunctionName)
  }

  def newFixtureWithSfpPool():Fixture = {
    val fxt = new Fixture()
    fxt.testActorRef ! fxt.newSfp
    fxt
  }
}

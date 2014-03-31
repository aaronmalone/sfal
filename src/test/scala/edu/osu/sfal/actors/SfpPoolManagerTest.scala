package edu.osu.sfal.actors

import org.scalatest.WordSpec
import edu.osu.sfal.messages.{SfpNotBusy, SfApplicationRequest}
import akka.testkit.{TestProbe, TestActorRef}
import akka.actor._
import java.util.HashMap
import org.apache.commons.lang3.RandomStringUtils
import edu.osu.sfal.util.{SfpName, SimulationFunctionName}
import edu.osu.sfal.messages.sfp.NewSfp
import com.google.common.collect.Sets
import edu.osu.sfal.actors.creators.SfpActorCreatorFactory

class SfpPoolManagerTest extends WordSpec {

  val system = ActorSystem.create("testSystem")

  class Fixture {
    val simulationFunctionName = new SimulationFunctionName(RandomStringUtils.randomAlphanumeric(7))
    private val actorCreatorFactory = new SfpActorCreatorFactory(null, simulationFunctionName)
    private val props = Props(classOf[SfpPoolManager], simulationFunctionName, actorCreatorFactory)
    val testActorRef = TestActorRef.create[SfpPoolManager](system, props)
    val underlyingActor: SfpPoolManager = testActorRef.underlyingActor
    testActorRef ! simulationFunctionName //tell the actor was SF it handles
    val sfpName = new SfpName(RandomStringUtils.randomAlphanumeric(7))
    val sfApplicationRequest = new SfApplicationRequest(simulationFunctionName, 0, new HashMap(), Sets.newHashSet())
    val testProbe = TestProbe.apply()(system)
    val newSfp = new NewSfp(simulationFunctionName, sfpName)
  }

  "An SfpPoolManager actor," when {

    "it receives an " + classOf[NewSfp].getName + " message," should {
      "create an actor and store its ref internally" in {
        val fxt = new Fixture()
        fxt.testActorRef ! fxt.newSfp
        val sfpPoolManager = fxt.underlyingActor
        val sfp = fxt.sfpName
        assert(sfpPoolManager.sfpActorMap.get(sfp).isInstanceOf[ActorRef])
        assert(sfpPoolManager.sfpBusyMap.containsKey(sfp))
      }
    }

    "it receives an " + classOf[SfApplicationRequest].getName + " message," should {
      "send the SfApplicationRequest to a free SfpActor, is one is available" in {
        val fxt = newTestFixtureWithRegisteredSfp()

        //ensure the underlying actor appears not busy
        fxt.underlyingActor.sfpBusyMap.put(fxt.sfpName, false)

        fxt.testActorRef ! fxt.sfApplicationRequest
        verifyRequestReceivedBySfpActor(fxt)

        val busy = fxt.underlyingActor.sfpBusyMap.get(fxt.sfpName)
        assert(busy, "SfpActor should be marked as busy")
        assert(fxt.underlyingActor.requestQueue.isEmpty)
      }
      "enqueue the SfpApplication if no free SfpActor instances are available" in {
        val fxt = newTestFixtureWithRegisteredSfp()

        //make the actor look busy
        fxt.underlyingActor.sfpBusyMap.put(fxt.sfpName, true)
        val queueRef = fxt.underlyingActor.requestQueue
        assert(queueRef.isEmpty)

        fxt.testActorRef ! fxt.sfApplicationRequest
        assert(queueRef.size === 1)
        assert(fxt.sfApplicationRequest === queueRef.poll())
      }
      "throw an exception when the pool is empty of SfpActors" in {
        val fxt = new Fixture()
        intercept[IllegalStateException] {
          fxt.testActorRef.receive(fxt.sfApplicationRequest)
        }
      }
    }

    "it receives a " + classOf[SfpNotBusy].getName + " message, " should {
      "dispatch a request, if one is enqueued" in {
        val fxt = newTestFixtureWithRegisteredSfp()
        val sfpNotBusy = new SfpNotBusy(fxt.simulationFunctionName, fxt.sfpName)

        //manually enqueue request
        fxt.underlyingActor.requestQueue.add(fxt.sfApplicationRequest)

        fxt.testActorRef ! sfpNotBusy

        verifyRequestReceivedBySfpActor(fxt)
        assert(fxt.underlyingActor.sfpBusyMap.get(fxt.sfpName), "actor should be marked as busy")
      }
      "mark the actor as not busy if no request is enqueued" in {
        val fxt = newTestFixtureWithRegisteredSfp()

        //manually mark as busy
        fxt.underlyingActor.sfpBusyMap.put(fxt.sfpName, true)

        fxt.testActorRef ! new SfpNotBusy(fxt.simulationFunctionName, fxt.sfpName)
        assert(false === fxt.underlyingActor.sfpBusyMap.get(fxt.sfpName))
      }
    }
  }

  def verifyRequestReceivedBySfpActor(fxt: Fixture): Unit = {
    val sfpActor = fxt.underlyingActor.sfpActorMap.get(fxt.sfpName)
    assert(sfpActor != null)
    val messageReceivedByActor = getLastMessageReceivedByActor(sfpActor, system)
    assert(fxt.sfApplicationRequest === messageReceivedByActor)
  }

  def newTestFixtureWithRegisteredSfp() = {
    val fxt = new Fixture()
    fxt.testActorRef ! fxt.newSfp
    fxt
  }
}

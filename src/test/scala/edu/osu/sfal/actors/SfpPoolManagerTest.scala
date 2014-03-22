package edu.osu.sfal.actors

import org.scalatest.{BeforeAndAfter, WordSpec}
import edu.osu.sfal.messages.{SfpActorRegistration, SfApplication, SfApplicationRequest, SfApplicationResult}
import akka.testkit.{TestProbe, TestActorRef}
import akka.actor.{Props, ActorSystem}
import scala.concurrent.duration._
import scala.concurrent.Promise
import java.util
import org.apache.commons.lang3.RandomStringUtils
import edu.osu.sfal.util.{SfpName, SimulationFunctionName}

class SfpPoolManagerTest extends WordSpec {

  val system = ActorSystem.create("testSystem")

  def newFixture() = new {
    val testActorRef = TestActorRef.create[SfpPoolManager](system, Props[SfpPoolManager])
    val underlyingActor = testActorRef.underlyingActor
    val simulationFunctionName = new SimulationFunctionName(RandomStringUtils.randomAlphanumeric(7))
    testActorRef ! simulationFunctionName
    val sfpName = new SfpName(RandomStringUtils.randomAlphanumeric(7))
    val sfApplication = new SfApplication(simulationFunctionName, 0, new util.HashMap())
    val sfApplicationRequest = new SfApplicationRequest(sfApplication, Promise[SfApplicationResult])
    private implicit val sys = system
    val testProbe = TestProbe()
    val sfpActorRegistration = new SfpActorRegistration(simulationFunctionName, sfpName, testProbe.ref)
  }

  "An SfpPoolManager actor," when {

    "it receives an " + classOf[SfpActorRegistration].getName + " message," should {
      "store the corresponding ActorRef internally" in {
        val fxt = newFixture()
        fxt.testActorRef ! fxt.sfpActorRegistration
        assert(fxt.testProbe.ref === fxt.underlyingActor.sfpActorMap.get(fxt.sfpName))
        assert(fxt.underlyingActor.sfpBusyMap.containsKey(fxt.sfpName))
      }
    }

    "it receives an " + classOf[SfApplicationRequest].getName + " message," should {
      "store the corresponding result promise in the result promise map" in {
        val fxt = newTestFixtureWithRegisteredSfp()

        //make the actor look busy
        fxt.underlyingActor.sfpBusyMap.put(fxt.sfpName, true)

        fxt.testActorRef ! fxt.sfApplicationRequest
        val promiseInMap = fxt.underlyingActor.resultPromiseMap.get(fxt.sfApplication)
        assert(fxt.sfApplicationRequest.getSfApplicationResultPromise === promiseInMap)
        fxt.testProbe.expectNoMsg(150 millis)
      }
      "send the SfApplication to a free SfpActor, is one is available" in {
        val fxt = newTestFixtureWithRegisteredSfp()

        //ensure the underlying actor appears not busy
        fxt.underlyingActor.sfpBusyMap.put(fxt.sfpName, false)

        fxt.testActorRef ! fxt.sfApplicationRequest
        fxt.testProbe.expectMsg(fxt.sfApplication)
        val busy = fxt.underlyingActor.sfpBusyMap.get(fxt.sfpName)
        assert(busy, "SfpActor should be marked as busy")
      }
      "enqueue the SfpApplication if no free SfpActor instances are available" in {
        val fxt = newTestFixtureWithRegisteredSfp()

        //make the actor look busy
        fxt.underlyingActor.sfpBusyMap.put(fxt.sfpName, true)
        val queueRef = fxt.underlyingActor.sfApplicationQueue
        assert(queueRef.isEmpty)

        fxt.testActorRef ! fxt.sfApplicationRequest
        assert(queueRef.size === 1)
        assert(fxt.sfApplication === queueRef.poll())
      }
      "do something (but what?!) when the pool is empty of SfpActors" in {
        //TODO IMPLEMENT
      }
    }

    "it receives an " + classOf[SfApplicationResult].getName + " message," should {
      val fxt = newTestFixtureWithRequest()
      val promise = fxt.underlyingActor.resultPromiseMap.get(fxt.sfApplication)

      "succeed the corresponding promise" in {
        assert(!promise.isCompleted, "Promise should not have been completed.")
        fxt.testActorRef ! new SfApplicationResult(fxt.sfApplication, fxt.sfpName, new util.HashMap())
        assert(promise.isCompleted, "Promise should have been completed.")
      }
      "remove the corresponding promise from the result promise map" in {
        assert(false == fxt.underlyingActor.resultPromiseMap.containsKey(fxt.sfApplication))
      }
      "send the next queued SfApplication request (if one is queued) to the sending actor" in {
        val fxt = newTestFixtureWithRequest()
        val newRequest = newSfAppicationRequest(fxt.simulationFunctionName)
        fxt.testActorRef ! newRequest
        assert(1 === fxt.underlyingActor.sfApplicationQueue.size(), "One request should be queued.")
        val result = new SfApplicationResult(fxt.sfApplication, fxt.sfpName, new util.HashMap())
        fxt.testProbe.expectNoMsg(150 millis)
        fxt.testActorRef.tell(result, fxt.testProbe.ref)
        fxt.testProbe.expectMsg(newRequest.getSfApplication)
        assert(fxt.underlyingActor.sfpBusyMap.get(fxt.sfpName), "SfpActor should appear busy.")
      }
      "change the running state of the sending actor if there are no requests queued" in {
        val fxt = newTestFixtureWithRequest()
        assert(fxt.underlyingActor.sfpBusyMap.get(fxt.sfpName))
        fxt.testActorRef ! new SfApplicationResult(fxt.sfApplication, fxt.sfpName, new util.HashMap())
        assert(!fxt.underlyingActor.sfpBusyMap.get(fxt.sfpName))
      }
    }

  }

  def newTestFixtureWithRequest() = {
    val fxt = newTestFixtureWithRegisteredSfp()
    fxt.testActorRef ! fxt.sfApplicationRequest
    fxt.testProbe.expectMsg(fxt.sfApplication)
    fxt
  }

  def newTestFixtureWithRegisteredSfp() = {
    val fxt = newFixture()
    fxt.testActorRef ! fxt.sfpActorRegistration
    fxt
  }

  def newSfAppicationRequest(simFunctionName: SimulationFunctionName) = {
    val sfApplication = new SfApplication(simFunctionName, 1, new util.HashMap())
    new SfApplicationRequest(sfApplication, Promise[SfApplicationResult])
  }
}

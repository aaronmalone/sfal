package edu.osu.sfal.actors

import edu.osu.sfal.messages.{SfpNotBusy, SfApplicationRequest}
import akka.testkit.{TestProbe, TestActorRef}
import akka.actor._
import java.util.HashMap
import edu.osu.sfal.messages.sfp.{HeartbeatFailed, NewSfp}
import com.google.common.collect.Sets
import edu.osu.sfal.util.SfpName

class SfpPoolManagerTest extends SfalActorTestBase {

  class SfpPoolManagerTestFixture extends SfalActorTestFixture {

    //this creates SfpActor instances that send messages to a test probe
    private val sfpActorPropsFactory = newSfpActorPropsFactory()

    private val props = Props(classOf[SfpPoolManager], simulationFunctionName, sfpActorPropsFactory)
    val testActorRef = TestActorRef.create[SfpPoolManager](system, props)
    val sfpPoolManager = testActorRef.underlyingActor
    val sfApplicationRequest = new SfApplicationRequest(simulationFunctionName, 0,
      new HashMap(), Sets.newHashSet())
    val newSfp = new NewSfp(simulationFunctionName, sfpName)
  }

  "An SfpPoolManager actor," when {
    "it receives an " + classOf[NewSfp].getName + " message," should {
      "create an actor and store its ref internally" in {
        new SfpPoolManagerTestFixture() {
          testActorRef ! newSfp
          assert(sfpPoolManager.sfpActorMap.get(sfpName).isInstanceOf[ActorRef])
          assert(sfpPoolManager.sfpBusyMap.containsKey(sfpName))
        }
      }
    }

    "it receives a " + classOf[HeartbeatFailed].getName + " message," should {
      "remove the corresponding SfpActor" in {
        new SfpPoolManagerTestFixture() {
          testActorRef ! newSfp //register the SFP
          assert(sfpPoolManager.sfpActorMap.get(sfpName).isInstanceOf[ActorRef])

          //add another SFP
          val anotherNewSfp: NewSfp = new NewSfp(simulationFunctionName, new SfpName("anotherSFP"))
          testActorRef ! anotherNewSfp
          assert(2 == sfpPoolManager.sfpActorMap.size())

          //send heartbeat failed for first SFP
          testActorRef ! new HeartbeatFailed(simulationFunctionName, sfpName)
          assert(!sfpPoolManager.sfpActorMap.containsKey(sfpName))
          assert(!sfpPoolManager.sfpBusyMap.containsKey(sfpName))
          assert(1 == sfpPoolManager.sfpActorMap.size())
        }
      }
      "shutdown if the failed SFP is the last SFP in the pool" in {
        new SfpPoolManagerTestFixture() {
          testActorRef ! newSfp //register the SFP
          val probe = TestProbe()
          probe watch testActorRef
          testActorRef ! new HeartbeatFailed(simulationFunctionName, sfpName)
          probe.expectTerminated(testActorRef)
        }
      }
    }

    "it receives an " + classOf[SfApplicationRequest].getName + " message," should {
      "send the SfApplicationRequest to a free SfpActor, is one is available" in {
        new SfpPoolManagerTestFixture() {
          testActorRef ! newSfp //register the SFP

          //ensure the underlying actor is not busy
          assert(sfpPoolManager.sfpBusyMap.get(sfpName) === false)

          //replace underlying ActorRef with test probe
          sfpPoolManager.sfpActorMap.put(sfpName, testActor)

          testActorRef ! sfApplicationRequest
          expectMsg(sfApplicationRequest)

          val busyState = sfpPoolManager.sfpBusyMap.get(sfpName)
          assert(busyState, "SfpActor should be marked busy.")
          assert(sfpPoolManager.requestQueue.isEmpty)
        }
      }
      "enqueue the SfpApplication if no free SfpActor instances are available" in {
        new SfpPoolManagerTestFixture() {
          testActorRef ! newSfp //register SFP

          //make the actor look busy
          sfpPoolManager.sfpBusyMap.put(sfpName, true)

          val queue = sfpPoolManager.requestQueue
          assert(queue.isEmpty)

          testActorRef ! sfApplicationRequest
          assert(queue.size === 1)
          assert(sfApplicationRequest === queue.poll())
        }
      }
      "throw an exception when the pool is empty of SfpActors" in {
        new SfpPoolManagerTestFixture() {
          intercept[IllegalStateException] {
            testActorRef receive sfApplicationRequest
          }
        }
      }
    }

    "it receives a " + classOf[SfpNotBusy].getName + " message, " should {
      "dispatch a request, if one is enqueued" in {
        new SfpPoolManagerTestFixture() {
          testActorRef ! newSfp //register SFP
          sfpPoolManager.sfpActorMap.put(sfpName, testActor) //replace actor ref w/ tes probe
          sfpPoolManager.requestQueue.add(sfApplicationRequest) //manually enqueue request

          val sfpNotBusy = new SfpNotBusy(simulationFunctionName, sfpName)
          testActorRef ! sfpNotBusy

          expectMsg(sfApplicationRequest)
          assert(sfpPoolManager.sfpBusyMap.get(sfpName), "actor should be marked busy")
        }
      }
      "mark the actor as not busy if no request is enqueued" in {
        new SfpPoolManagerTestFixture() {
          testActorRef ! newSfp //register SFP
          sfpPoolManager.sfpBusyMap.put(sfpName, true) //manually mark as busy
          testActorRef ! new SfpNotBusy(simulationFunctionName, sfpName)
          assert(false === sfpPoolManager.sfpBusyMap.get(sfpName))
        }
      }
    }
  }
}

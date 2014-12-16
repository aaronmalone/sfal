package edu.osu.sfal.actors

import edu.osu.sfal.messages.SfApplicationRequest
import akka.testkit.{TestProbe, TestActorRef}
import akka.actor.Props
import edu.osu.sfal.util.SfpName
import edu.osu.sfal.messages.sfp.NewSfpMsg
import java.util.HashMap
import com.google.common.collect.Sets
import edu.osu.sfal.data.InputValuesMap

class SfpGeneralManagerTest extends SfalActorTestBase {

  class SfpGeneralManagerTestFixture extends SfalActorTestFixture {
    private val props = Props(classOf[SfpGeneralManager], mockLapisApi)
    val testActorRef = TestActorRef.create[SfpGeneralManager](system, props)
    val sfpGeneralManager = testActorRef.underlyingActor
    assert(sfpGeneralManager.sfpPoolMap.isEmpty)
    val newSfp = new NewSfpMsg(simulationFunctionName, sfpName)
    lazy val sfpPoolManagerActor = sfpGeneralManager.sfpPoolMap.get(simulationFunctionName)
  }

  "The SfpGeneralManager" when {
    "it receives a " + classOf[NewSfpMsg].getName + " message," should {
      "should create a new SfpPoolManager if none exists for the simulation function " +
        "and dispatch the NewSfp message to the SfpPoolManager" in {
        new SfpGeneralManagerTestFixture() {
          testActorRef ! newSfp
          assert(sfpPoolManagerActor != null)
        }
      }
    }
    "it receives a " + classOf[NewSfpMsg].getName + " message for an existing SFP pool," should {
      "dispatch the message to the corresponding SfpPoolManager" in {
        new SfpGeneralManagerTestFixture() {
          testActorRef ! newSfp
          assert(sfpGeneralManager.sfpPoolMap.size === 1)
          //one in map
          val testProbe = TestProbe()
          sfpGeneralManager.sfpPoolMap.put(simulationFunctionName, testProbe.ref)
          val secondNewSfpMessage = new NewSfpMsg(simulationFunctionName, new SfpName("secondSFP"))
          testActorRef ! secondNewSfpMessage
          testProbe.expectMsg(secondNewSfpMessage)
          assert(sfpGeneralManager.sfpPoolMap.size === 1) //no new SfpPoolManager actors added
        }
      }
    }
    "it receives a " + classOf[SfApplicationRequest].getName + " message, " should {
      "forward to the appropriate SfpPoolManager" in {
        new SfpGeneralManagerTestFixture() {
          mockFlagCall("readyToCalculate", false)
          mockFlagCall("finishedCalculating", true)
          testActorRef ! newSfp
          val request = new SfApplicationRequest(simulationFunctionName, 0, new InputValuesMap, Sets.newHashSet())
          val testProbe = TestProbe()
          sfpGeneralManager.sfpPoolMap.put(simulationFunctionName, testProbe.ref)
          testActorRef ! request
          testProbe.expectMsg(request)
        }
      }
    }
  }
}

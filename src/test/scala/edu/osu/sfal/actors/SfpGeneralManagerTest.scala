package edu.osu.sfal.actors

import edu.osu.sfal.messages.SfApplicationRequest
import akka.testkit.TestActorRef
import akka.actor.Props
import edu.osu.sfal.util.SfpName
import edu.osu.sfal.messages.sfp.NewSfp
import java.util.HashMap
import com.google.common.collect.Sets

class SfpGeneralManagerTest extends SfalActorTestBase {

  class SfpGeneralManagerTestFixture extends SfalActorTestFixture {

    private val sfpPoolManagerPropsFactory = new SfpPoolManagerPropsFactory(sfpActorPropsFactory) {
      protected override def getArgsToUse(argsPassedIn: Array[AnyRef]): Array[AnyRef] = {
        Array(simulationFunctionName, sfpActorPropsFactory)
      }
    }
    private val props = Props(classOf[SfpGeneralManager], sfpPoolManagerPropsFactory)
    val testActorRef = TestActorRef.create[SfpGeneralManager](system, props)
    val sfpGeneralManager = testActorRef.underlyingActor
    assert(sfpGeneralManager.sfpPoolMap.isEmpty)
    val newSfp = new NewSfp(simulationFunctionName, sfpName)
    lazy val sfpPoolManagerActor = sfpGeneralManager.sfpPoolMap.get(simulationFunctionName)
  }

  "The SfpGeneralManager" when {
    "it receives a " + classOf[NewSfp].getName + " message," should {
      "should create a new SfpPoolManager if none exists for the simulation function " +
          "and dispatch the NewSfp message to the SfpPoolManager" in {
        new SfpGeneralManagerTestFixture() {
          testActorRef ! newSfp
          assert(sfpPoolManagerActor != null)
          assert(newSfp === getLastMessageReceivedByActor(sfpPoolManagerActor, system))
        }
      }
    }
    "it receives a " + classOf[NewSfp].getName + " message for an existing SFP pool," should {
      "dispatch the message to the corresponding SfpPoolManager" in {
        new SfpGeneralManagerTestFixture() {
          testActorRef ! newSfp
          val secondNewSfpMessage = new NewSfp(simulationFunctionName, new SfpName("secondSFP"))
          testActorRef ! secondNewSfpMessage
          assert(sfpGeneralManager.sfpPoolMap.size === 1) //no new SfpPoolManager actors added
          val lastMsgReceived = getLastMessageReceivedByActor(sfpPoolManagerActor, system)
          assert(secondNewSfpMessage === lastMsgReceived)
        }
      }
    }
    "it receives a " + classOf[SfApplicationRequest].getName + " message, " should {
      "forward to the appropriate SfpPoolManager" in {
        new SfpGeneralManagerTestFixture() {
          mockFlagCall("readyToCalculate", false)
          mockFlagCall("finishedCalculating", true)
          testActorRef ! newSfp
          val request = new SfApplicationRequest(simulationFunctionName, 0, new HashMap(), Sets.newHashSet())
          testActorRef ! request
          assert(request === getLastMessageReceivedByActor(sfpPoolManagerActor, system))
        }
      }
    }
  }
}

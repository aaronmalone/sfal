package edu.osu.sfal.actors

import akka.actor.{Terminated, Props}
import akka.testkit.TestActorRef
import org.mockito.Mockito
import edu.osu.lapis.Flags
import edu.osu.sfal.messages.{SfApplicationResult, SfApplicationRequest}
import scala.collection.JavaConversions.{mapAsJavaMap,setAsJavaSet}
import edu.osu.sfal.messages.sfp.HeartbeatFailed


class SfpActorTest extends SfalActorTestBase {

  val FlagFalse = Flags.FLAG_VALUE_FALSE
  val FlagTrue = Flags.FLAG_VALUE_TRUE
  val ReadyToCalculate = SfpActor.READY_TO_CALCULATE_VAR_NAME
  val FinishedCalculating = SfpActor.FINISHED_CALCULATING_VAR_NAME

  /*
       readyToCalculate      finishedCalculating       meaning
       false                 true                      before a calculation has begun, input variables can be set and output variables retrieved
       true                  true                      should only happen immediately after readyToCalculate is set to true
       true                  false                     the SFP will then set its finishedCalculating to false
       false                 false                     the SFP will next set its readyToCalculate to false, then run calculations
       false                 true                      when calculation has finished
   */

  class SfpActorTestFixture extends SfalActorTestFixture {
    val nodeName = sfpName.getName
    private val props = Props.create(classOf[SfpActor], simulationFunctionName, sfpName, mockLapisApi,
      testActor, testActor, testActor)
    val testActorRef = TestActorRef.create[SfpActor](system, props)
    val sfpActor = testActorRef.underlyingActor

    expectMsg(SfpActor.HEARTBEAT_MSG) // heartbeat check triggered on construction

    Mockito.when(mockLapisApi.getArrayOfDouble(nodeName, ReadyToCalculate)).thenReturn(FlagFalse)
    Mockito.when(mockLapisApi.getArrayOfDouble(nodeName, FinishedCalculating)).thenReturn(FlagTrue)
  }

  new SfpActorTestFixture() {

    val inputsMap: Map[String, AnyRef] = Map("input1"->"INPUT_VALUE1", "input2"->"INPUT_VALUE_2")
    val outputNamesSet = Set("output1")
    val request = new SfApplicationRequest(simulationFunctionName, 0, inputsMap, outputNamesSet)

    "When an SFP actor receives a request message, it " should {
      "save the request as the currentRequest member" in {

        testActorRef ! request
        assert(request === sfpActor.getCurrentRequest)
      }
      "validate that the SFP is not currently working" in {
        Mockito.verify(mockLapisApi).getArrayOfDouble(nodeName, ReadyToCalculate)
        Mockito.verify(mockLapisApi).getArrayOfDouble(nodeName, FinishedCalculating)
      }
      "set the input variables on the SFP" in {
        inputsMap.foreach {
          case (name, value) => Mockito.verify(mockLapisApi).set(nodeName, name, value)
        }
      }
      "set the readyToCalculate flag on the SFP" in {
        Mockito.verify(mockLapisApi).set(nodeName, ReadyToCalculate, FlagTrue)
      }
      "schedule a check on the calculation" in {
        expectMsg(SfpActor.CHECK_ON_CALCULATION)
      }
    }

    "When an SFP actor receives a check-on-calculation message, it " should {
      "check whether the calculation has finished" in {
        Mockito.reset(mockLapisApi)
        Mockito.when(mockLapisApi.getArrayOfDouble(nodeName, FinishedCalculating)).thenReturn(FlagFalse)
        testActorRef ! SfpActor.CHECK_ON_CALCULATION
        Mockito.verify(mockLapisApi).getArrayOfDouble(nodeName, FinishedCalculating)
      }
      "schedule another check on the calculation" in {
        expectMsg(SfpActor.CHECK_ON_CALCULATION)
      }
    }
  }

  new SfpActorTestFixture() {

    val outputName = "outputName1"
    val map: Map[String,AnyRef] = Map()
    val set: Set[String] = Set(outputName)
    val request = new SfApplicationRequest(simulationFunctionName, 0, map, set)

    "When an SFP actor checks on a finished calculation, it" should {
      "retrieve the output values" in {
        testActorRef ! request //this will set the current request

        expectMsg(SfpActor.CHECK_ON_CALCULATION)
        Mockito.reset(mockLapisApi)
        Mockito.when(mockLapisApi.getArrayOfDouble(nodeName, FinishedCalculating)).thenReturn(FlagTrue)

        testActorRef ! SfpActor.CHECK_ON_CALCULATION

        Mockito.verify(mockLapisApi).getArrayOfDouble(nodeName, FinishedCalculating)
        Mockito.verify(mockLapisApi).getObject(nodeName, outputName)
        Mockito.verifyNoMoreInteractions(mockLapisApi)
      }
      "complete the corresponding request" in {
        assert(request.getCompletableFuture.isDone)
        assert(request.getCompletableFuture.get().isInstanceOf[SfApplicationResult])
        val result = request.getCompletableFuture.get()
        assert(result.getOutputs.containsKey(outputName))
      }
      "clear the current request field" in {
        assert(sfpActor.getCurrentRequest === null)
      }
    }
  }

  //todo DRY-out some of the mocking code for flag calls

  new SfpActorTestFixture() {

    val request = new SfApplicationRequest(simulationFunctionName, 0, Map[String,Object](), Set[String]())

    "When an SFP actor receives a heartbeat message, it" should {
      "check whether the corresponding LAPIS node is alive" in {

        /* set up */
        testActorRef ! request
        expectMsg(SfpActor.CHECK_ON_CALCULATION)

        Mockito.when(mockLapisApi.doHeartbeatCheckReturnNodeIsLive(nodeName)).thenReturn(true)
        testActorRef ! SfpActor.HEARTBEAT_MSG
        Mockito.verify(mockLapisApi).doHeartbeatCheckReturnNodeIsLive(nodeName)
      }
      "schedule another heartbeat check if the node is alive" in {
        expectMsg(SfpActor.HEARTBEAT_MSG)
      }
      "send a heartbeat failed message if the heartbeat indicates the node is not alive" in {
        Mockito.when(mockLapisApi.doHeartbeatCheckReturnNodeIsLive(nodeName)).thenReturn(false)
        watch(testActorRef)
        testActorRef ! SfpActor.HEARTBEAT_MSG
        expectMsg(new HeartbeatFailed(simulationFunctionName, sfpName))
      }
      "shut down if the node is not alive" in {
        val terminated = expectMsgClass(classOf[Terminated])
        assert(testActorRef === terminated.actor)
      }
      "complete the completable future with an exception if the node is not alive" in {
        assert(request.getCompletableFuture.isCompletedExceptionally)
      }
    }
  }

}

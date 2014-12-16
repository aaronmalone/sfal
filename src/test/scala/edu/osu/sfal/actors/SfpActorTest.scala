package edu.osu.sfal.actors

import akka.actor.{Terminated, Props}
import akka.testkit.TestActorRef
import org.mockito.Mockito.{when, verify, reset, verifyNoMoreInteractions}
import edu.osu.lapis.Flags
import edu.osu.sfal.messages.{Messages, SfApplicationResult, SfApplicationRequest}
import scala.collection.JavaConversions.{mapAsJavaMap, setAsJavaSet}
import edu.osu.sfal.messages.sfp.HeartbeatFailedMsg
import edu.osu.sfal.data.InputValuesMap


class SfpActorTest extends SfalActorTestBase {

  val ReadyToCalculate = SfpActor.READY_TO_CALCULATE_VAR_NAME
  val FinishedCalculating = SfpActor.FINISHED_CALCULATING_VAR_NAME

  class SfpActorTestFixture extends SfalActorTestFixture {
    SfpActor.setHeartbeatPeriodMillis(15)
    SfpActor.setCalculationCheckPeriodMillis(15)
    val nodeName = sfpName.getName
    private val props = Props.create(classOf[SfpActor], simulationFunctionName, sfpName, mockLapisApi, testActor)
    val testActorRef = TestActorRef.create[SfpActor](system, props)
    val sfpActor = testActorRef.underlyingActor

    expectMsg(Messages.HEARTBEAT_MSG) // heartbeat check triggered on construction

    mockFlagCall(ReadyToCalculate, flagValue = false)
    mockFlagCall(FinishedCalculating, flagValue = true)
  }

  new SfpActorTestFixture() {

    val inputsMap: Map[String, AnyRef] = Map("input1" -> "INPUT_VALUE1", "input2" -> "INPUT_VALUE_2")
    val inputs = InputValuesMap.fromMap(inputsMap)
    val outputNamesSet = Set("output1")
    val request = new SfApplicationRequest(simulationFunctionName, 0, inputs, outputNamesSet)

    "When an SFP actor receives a request message, it " should {
      "save the request as the currentRequest member" in {

        testActorRef ! request
        assert(request === sfpActor.getCurrentRequest)
      }
      "validate that the SFP is not currently working" in {
        verify(mockLapisApi).getArrayOfDouble(nodeName, ReadyToCalculate)
        verify(mockLapisApi).getArrayOfDouble(nodeName, FinishedCalculating)
      }
      "set the input variables on the SFP" in {
        inputsMap.foreach {
          case (name, value) => verify(mockLapisApi).set(nodeName, name, value)
        }
      }
      "set the readyToCalculate flag on the SFP" in {
        verify(mockLapisApi).set(nodeName, ReadyToCalculate, Flags.getFlagTrue)
      }
      "schedule a check on the calculation" in {
        expectMsg(Messages.CHECK_ON_CALCULATION)
      }
    }

    "When an SFP actor receives a check-on-calculation message, it " should {
      "check whether the calculation has finished" in {
        reset(mockLapisApi)
        mockFlagCall(FinishedCalculating, flagValue = false)
        testActorRef ! Messages.CHECK_ON_CALCULATION
        verify(mockLapisApi).getArrayOfDouble(nodeName, FinishedCalculating)
      }
      "schedule another check on the calculation" in {
        expectMsg(Messages.CHECK_ON_CALCULATION)
      }
    }
  }

  new SfpActorTestFixture() {

    val outputName = "outputName1"
    val set: Set[String] = Set(outputName)
    val request = new SfApplicationRequest(simulationFunctionName, 0, new InputValuesMap, set)

    "When an SFP actor checks on a finished calculation, it" should {
      "retrieve the output values" in {
        testActorRef ! request //this will set the current request

        expectMsg(Messages.CHECK_ON_CALCULATION)
        reset(mockLapisApi)
        mockFlagCall(FinishedCalculating, flagValue = true)
        mockFlagCall(ReadyToCalculate, flagValue = false)
        when(mockLapisApi.getObject(nodeName, outputName)).thenReturn(new Object(), null)

        testActorRef ! Messages.CHECK_ON_CALCULATION

        verify(mockLapisApi).getArrayOfDouble(nodeName, FinishedCalculating)
        verify(mockLapisApi).getArrayOfDouble(nodeName, ReadyToCalculate)
        verify(mockLapisApi).getObject(nodeName, outputName)
        verifyNoMoreInteractions(mockLapisApi)
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

  new SfpActorTestFixture() {

    val request = new SfApplicationRequest(simulationFunctionName, 0, new InputValuesMap, Set[String]())

    "When an SFP actor receives a heartbeat message, it" should {
      "check whether the corresponding LAPIS node is alive" in {

        /* set up currentRequest so we can complete exceptionally later */
        testActorRef ! request
        expectMsg(Messages.CHECK_ON_CALCULATION)

        when(mockLapisApi.doHeartbeatCheckReturnNodeIsLive(nodeName)).thenReturn(true)
        testActorRef ! Messages.HEARTBEAT_MSG
        verify(mockLapisApi).doHeartbeatCheckReturnNodeIsLive(nodeName)
      }
      "schedule another heartbeat check if the node is alive" in {
        expectMsg(Messages.HEARTBEAT_MSG)
      }
      "send a heartbeat failed message if the heartbeat indicates the node is not alive" in {
        when(mockLapisApi.doHeartbeatCheckReturnNodeIsLive(nodeName)).thenReturn(false)
        watch(testActorRef)
        testActorRef ! Messages.HEARTBEAT_MSG
        expectMsg(new HeartbeatFailedMsg(simulationFunctionName, sfpName))
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

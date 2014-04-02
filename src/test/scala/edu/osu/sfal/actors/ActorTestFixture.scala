package edu.osu.sfal.actors

import org.apache.commons.lang3.RandomStringUtils
import edu.osu.sfal.util.{SimulationFunctionName, SfpName}
import edu.osu.lapis.{Flags, LapisApi}
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.mockito.Matchers
import edu.osu.sfal.actors.creators.SfpActorCreatorFactory

class ActorTestFixture {
  val simulationFunctionName = new SimulationFunctionName(randomString())
  val sfpName = new SfpName(randomString())
  val mockLapisApi = getMockLapisApi(simulationFunctionName)
  val sfpActorCreatorFactory = new SfpActorCreatorFactory(mockLapisApi, simulationFunctionName)

  def getMockLapisApi(simulationFunctionName: SimulationFunctionName): LapisApi = {
    val mockLapisApi = mock(classOf[LapisApi])
    val simFunctionNameString = simulationFunctionName.getName
    when(mockLapisApi.getString(anyString(), Matchers.eq("SIMULATION_FUNCTION_NAME"))).thenReturn(simFunctionNameString)
    when(mockLapisApi.doHeartbeatCheckReturnNodeIsLive(anyString())).thenReturn(true)
    when(mockLapisApi.getArrayOfDouble(anyString(), Matchers.eq("finishedCalculating"))).thenReturn(Flags.FLAG_VALUE_TRUE)
    mockLapisApi
  }

  private def randomString() = RandomStringUtils.randomAlphanumeric(10)
}

package edu.osu.sfal.actors

import org.apache.commons.lang3.RandomStringUtils
import edu.osu.sfal.util.{SimulationFunctionName, SfpName}
import edu.osu.lapis.LapisApi
import org.mockito.Mockito._
import edu.osu.sfal.actors.creators.SfpActorCreatorFactory
import akka.actor.ActorSystem
import org.mockito.Matchers.anyString

class SfalActorTestFixture(implicit system: ActorSystem) {
  val simulationFunctionName = new SimulationFunctionName("name_"+randomString())
  val sfpName = new SfpName("sfp_"+randomString())
  val mockLapisApi = mock(classOf[LapisApi])

  val sfpActorCreatorFactory = new SfpActorCreatorFactory(mockLapisApi, simulationFunctionName)

  //when SfpActor instances are constructed, they start a heartbeat check... we don't want this to fail in test
  when(mockLapisApi.doHeartbeatCheckReturnNodeIsLive(anyString())).thenReturn(true)


  private def randomString() = RandomStringUtils.randomAlphanumeric(10)
}

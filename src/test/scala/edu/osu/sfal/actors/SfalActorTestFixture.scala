package edu.osu.sfal.actors

import org.apache.commons.lang3.RandomStringUtils
import edu.osu.sfal.util.{SimulationFunctionName, SfpName}
import edu.osu.lapis.{Flags, LapisApi}
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.mockito.Matchers
import edu.osu.sfal.actors.creators.SfpActorCreatorFactory

class SfalActorTestFixture {
  val simulationFunctionName = new SimulationFunctionName("name_"+randomString())
  val sfpName = new SfpName("sfp_"+randomString())
  val mockLapisApi = mock(classOf[LapisApi])
  val sfpActorCreatorFactory = new SfpActorCreatorFactory(mockLapisApi, simulationFunctionName)

  private def randomString() = RandomStringUtils.randomAlphanumeric(10)
}

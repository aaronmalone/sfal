package edu.osu.sfal.actors

import org.apache.commons.lang3.RandomStringUtils
import edu.osu.sfal.util.{SimulationFunctionName, SfpName}
import edu.osu.lapis.{Flags, LapisApi}
import org.mockito.Mockito._
import akka.actor.{Props, ActorSystem}
import org.mockito.Matchers.anyString
import akka.testkit.TestProbe

class SfalActorTestFixture(implicit system: ActorSystem) {
  val simulationFunctionName = new SimulationFunctionName("name_"+randomString())
  val sfpName = new SfpName("sfp_"+randomString())
  val mockLapisApi = mock(classOf[LapisApi])

  //when SfpActor instances are constructed, they start a heartbeat check... we don't want this to fail in test
  when(mockLapisApi.doHeartbeatCheckReturnNodeIsLive(anyString())).thenReturn(true)

  /**
   * A PropsFactory for creating SfpActor instances that send messages to a test probe
   */
  lazy val sfpActorPropsFactory: PropsFactory[SfpActor] =
    new SfpActorPropsFactory(mockLapisApi) {
      protected override def getArgsToUse(argsPassedIn: Array[AnyRef]) = {
        Array(simulationFunctionName, sfpName, mockLapisApi, TestProbe().ref);
      }
    }

  private def randomString() = RandomStringUtils.randomAlphanumeric(10)

  def mockFlagCall(flagName: String, flagValue: Boolean): Unit = {
    when(mockLapisApi.getArrayOfDouble(sfpName.getName, flagName)).thenReturn(Flags.getFlag(flagValue))
  }
}

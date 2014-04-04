package edu.osu.sfal.actors

import edu.osu.sfal.util.{SfpName, SimulationFunctionName}
import edu.osu.lapis.LapisApi
import akka.actor.ActorSystem
import akka.japi.Creator
import edu.osu.sfal.actors.creators.SfpActorCreatorFactory
import akka.testkit.TestProbe

class TestSfpActorCreatorFactory(
    lapisApi: LapisApi,
    simFunName: SimulationFunctionName)
    (implicit system: ActorSystem)
  extends SfpActorCreatorFactory(lapisApi, simFunName) {
  override def createCreator(sfp: SfpName): Creator[SfpActor] = {
    new Creator[SfpActor]() {
      override def create(): SfpActor = new SfpActor(simFunName, sfp, lapisApi, TestProbe().ref);
    }
  }
}

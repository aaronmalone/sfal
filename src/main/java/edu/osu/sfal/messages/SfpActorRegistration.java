package edu.osu.sfal.messages;

import akka.actor.ActorRef;
import edu.osu.sfal.util.SfpName;
import edu.osu.sfal.util.SimulationFunctionName;

public class SfpActorRegistration {
    private final SimulationFunctionName simulationFunctionName;
    private final SfpName sfpName;
    private final ActorRef actorRef;

    public SfpActorRegistration(SimulationFunctionName simulationFunctionName, SfpName sfpName, ActorRef actorRef) {
        this.simulationFunctionName = simulationFunctionName;
        this.sfpName = sfpName;
        this.actorRef = actorRef;
    }

    public SimulationFunctionName getSimulationFunctionName() {
        return simulationFunctionName;
    }

    public SfpName getSfpName() {
        return sfpName;
    }

    public ActorRef getActorRef() {
        return actorRef;
    }
}

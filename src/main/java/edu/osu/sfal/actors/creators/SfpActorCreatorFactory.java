package edu.osu.sfal.actors.creators;

import akka.japi.Creator;
import edu.osu.lapis.LapisApi;
import edu.osu.sfal.actors.SfpActor;
import edu.osu.sfal.util.SfpName;
import edu.osu.sfal.util.SimulationFunctionName;

public class SfpActorCreatorFactory {
	private final LapisApi lapisApi;
	private final SimulationFunctionName simulationFunctionName;

	public SfpActorCreatorFactory(LapisApi lapisApi, SimulationFunctionName simulationFunctionName) {
		this.lapisApi = lapisApi;
		this.simulationFunctionName = simulationFunctionName;
	}

	public Creator<SfpActor> createCreator(SfpName sfpName) {
		return () -> new SfpActor(simulationFunctionName, sfpName, lapisApi);
	}

	public LapisApi getLapisApi() {
		return lapisApi;
	}

	public SimulationFunctionName getSimulationFunctionName() {
		return simulationFunctionName;
	}
}

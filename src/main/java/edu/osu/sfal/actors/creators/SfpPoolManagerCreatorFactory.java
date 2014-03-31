package edu.osu.sfal.actors.creators;

import akka.japi.Creator;
import edu.osu.lapis.LapisApi;
import edu.osu.sfal.actors.SfpPoolManager;
import edu.osu.sfal.util.SimulationFunctionName;

public class SfpPoolManagerCreatorFactory {

	private final LapisApi lapisApi;

	public SfpPoolManagerCreatorFactory(LapisApi lapisApi) {
		this.lapisApi = lapisApi;
	}

	public Creator<SfpPoolManager> createCreator(SimulationFunctionName simFunctionName) {
		return () -> {
			SfpActorCreatorFactory sfpCreatorFactory = new SfpActorCreatorFactory(lapisApi, simFunctionName);
			return new SfpPoolManager(simFunctionName, sfpCreatorFactory);
		};
	}
}

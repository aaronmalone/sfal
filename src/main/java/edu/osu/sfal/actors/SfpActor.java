package edu.osu.sfal.actors;

import edu.osu.lapis.LapisApi;
import edu.osu.sfal.util.SfpName;
import edu.osu.sfal.util.SimulationFunctionName;

public class SfpActor extends LastMessageReceivedActor {

	private final SimulationFunctionName simulationFunctionName;
	private final SfpName sfpName;
	private final LapisApi lapisApi;

	public SfpActor(SimulationFunctionName simulationFunctionName, SfpName sfpName, LapisApi lapisApi) {
		this.simulationFunctionName = simulationFunctionName;
		this.lapisApi = lapisApi;
		this.sfpName = sfpName;
	}

	@Override public void onReceiveImpl(Object message) throws Exception {

	}
}

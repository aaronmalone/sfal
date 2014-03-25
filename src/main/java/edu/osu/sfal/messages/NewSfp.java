package edu.osu.sfal.messages;

import edu.osu.sfal.util.SfpName;
import edu.osu.sfal.util.SimulationFunctionName;

public class NewSfp{

	private final SimulationFunctionName simulationFunctionName;
	private final SfpName sfpName;

	public NewSfp(SimulationFunctionName simulationFunctionName, SfpName sfpName) {
		this.simulationFunctionName = simulationFunctionName;
		this.sfpName = sfpName;
	}

	public SimulationFunctionName getSimulationFunctionName() {
		return simulationFunctionName;
	}

	public SfpName getSfpName() {
		return sfpName;
	}
}

package edu.osu.sfal.messages.sfp;

import edu.osu.sfal.util.SfpName;
import edu.osu.sfal.util.SimulationFunctionName;

public abstract class SfpStatusMessage {
	private final SimulationFunctionName simulationFunctionName;
	private final SfpName sfpName;

	public SfpStatusMessage(SimulationFunctionName simulationFunctionName, SfpName sfpName) {
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

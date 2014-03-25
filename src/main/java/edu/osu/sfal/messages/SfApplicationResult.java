package edu.osu.sfal.messages;

import edu.osu.sfal.util.SfpName;
import edu.osu.sfal.util.SimulationFunctionName;

import java.util.Map;

public class SfApplicationResult {
	private final SimulationFunctionName simulationFunctionName;
	private final int timestep;
	private final Map<String, Object> outputs;
	private final SfpName sfpName;

	public SfApplicationResult(SimulationFunctionName simulationFunctionName, int timestep,
			Map<String, Object> outputs, SfpName sfpName) {
		this.simulationFunctionName = simulationFunctionName;
		this.timestep = timestep;
		this.outputs = outputs;
		this.sfpName = sfpName;
	}

	public SimulationFunctionName getSimulationFunctionName() {
		return simulationFunctionName;
	}

	public int getTimestep() {
		return timestep;
	}

	public Map<String, Object> getOutputs() {
		return outputs;
	}

	public SfpName getSfpName() {
		return sfpName;
	}
}

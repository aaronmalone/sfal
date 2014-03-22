package edu.osu.sfal.messages;

import edu.osu.sfal.util.SimulationFunctionName;

import java.util.Map;

public class SfApplication {
	
	private final SimulationFunctionName simulationFunctionName;
	private final int timestep;
	private final Map<String, Object> inputs;

	public SfApplication(SimulationFunctionName simulationFunctionName, int timestep, Map<String, Object> inputs) {
		this.simulationFunctionName = simulationFunctionName;
		this.timestep = timestep;
		this.inputs = inputs;
	}

	public SimulationFunctionName getSimulationFunctionName() {
		return simulationFunctionName;
	}

	public int getTimestep() {
		return timestep;
	}

	public Map<String, Object> getInputs() {
		return inputs;
	}
}

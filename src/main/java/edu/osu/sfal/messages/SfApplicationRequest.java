package edu.osu.sfal.messages;

import edu.osu.sfal.util.SimulationFunctionName;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class SfApplicationRequest {
	
	private final SimulationFunctionName simulationFunctionName;
	private final int timestep;
	private final Map<String, Object> inputs;
	private final Set<String> outputNames;
	private final CompletableFuture<SfApplicationResult> completableFuture;

	public SfApplicationRequest(SimulationFunctionName simulationFunctionName, int timestep,
			Map<String, Object> inputs, Set<String> outputNames) {
		this(simulationFunctionName, timestep, inputs, outputNames, new CompletableFuture<>());
	}

	public SfApplicationRequest(SimulationFunctionName simulationFunctionName, int timestep,
			Map<String, Object> inputs, Set<String> outputNames,
			CompletableFuture<SfApplicationResult> completableFuture) {
		this.simulationFunctionName = simulationFunctionName;
		this.timestep = timestep;
		this.inputs = inputs;
		this.outputNames = outputNames;
		this.completableFuture = completableFuture;
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

	public CompletableFuture<SfApplicationResult> getCompletableFuture() {
		return completableFuture;
	}

	public Set<String> getOutputNames() {
		return outputNames;
	}
}

package edu.osu.sfal.messages;

import edu.osu.sfal.data.InputValuesMap;
import edu.osu.sfal.util.SimulationFunctionName;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * A request to run one execution of a simulation function.
 */
public class SfApplicationRequest {

	private final SimulationFunctionName simulationFunctionName;
	private final int timestep;
	private final InputValuesMap inputs;
	private final Set<String> outputNames;
	private final CompletableFuture<SfApplicationResult> completableFuture;

	public SfApplicationRequest(SimulationFunctionName simulationFunctionName, int timestep,
			InputValuesMap inputs, Set<String> outputNames) {
		this(simulationFunctionName, timestep, inputs, outputNames, new CompletableFuture<>());
	}

	public SfApplicationRequest(SimulationFunctionName simulationFunctionName, int timestep,
			InputValuesMap inputs, Set<String> outputNames,
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

	public InputValuesMap getInputs() {
		return inputs;
	}

	public CompletableFuture<SfApplicationResult> getCompletableFuture() {
		return completableFuture;
	}

	/**
	 * Set of output variables (published LAPIS variables).
	 */
	public Set<String> getOutputNames() {
		return outputNames;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(" + simulationFunctionName.getName()
				+ ", " + timestep + ")";
	}
}

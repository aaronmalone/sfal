package edu.osu.sfal.messages;

import edu.osu.sfal.util.SimulationFunctionName;
import scala.concurrent.Promise;
import scala.concurrent.Promise$;

import java.util.Map;

public class SfApplicationRequest {
	
	private final SimulationFunctionName simulationFunctionName;
	private final int timestep;
	private final Map<String, Object> inputs;
	private final Promise<SfApplicationResult> promise;

	public SfApplicationRequest(SimulationFunctionName simulationFunctionName, int timestep, Map<String, Object> inputs) {
		this(simulationFunctionName, timestep, inputs, getNewPromise());
	}

	public SfApplicationRequest(SimulationFunctionName simulationFunctionName, int timestep,
			Map<String, Object> inputs, Promise<SfApplicationResult> promise) {
		this.simulationFunctionName = simulationFunctionName;
		this.timestep = timestep;
		this.inputs = inputs;
		this.promise = promise;
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

	public Promise<SfApplicationResult> getPromise() {
		return promise;
	}


	private static Promise<SfApplicationResult> getNewPromise() {
		//why use this method? Java was struggling with inferring the type of Promise$.MODULE$.apply()
		return Promise$.MODULE$.apply();
	}
}

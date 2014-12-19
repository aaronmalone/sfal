package edu.osu.sfal.exception;

import edu.osu.sfal.util.SimulationFunctionName;

/**
 * Exception indicating that a request to execute a simulation function cannot be
 * completed because no SFP capable of executing that function has registered
 * itself with the SFAL.
 */
public class NoSfpAvailableException extends RuntimeException {

	private final SimulationFunctionName simulationFunctionName;

	public NoSfpAvailableException(SimulationFunctionName simulationFunctionName) {
		this.simulationFunctionName = simulationFunctionName;
	}

	public NoSfpAvailableException(SimulationFunctionName simulationFunctionName, String message) {
		super(message);
		this.simulationFunctionName = simulationFunctionName;
	}

	public NoSfpAvailableException(SimulationFunctionName simulationFunctionName, String message, Throwable cause) {
		super(message, cause);
		this.simulationFunctionName = simulationFunctionName;
	}

	public NoSfpAvailableException(SimulationFunctionName simulationFunctionName, Throwable cause) {
		super(cause);
		this.simulationFunctionName = simulationFunctionName;
	}
}

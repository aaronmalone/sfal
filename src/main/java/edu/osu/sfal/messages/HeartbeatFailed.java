package edu.osu.sfal.messages;

public class HeartbeatFailed {
	private final String simulationFunctionName;
	private final String nodeIdentifier;
	
	public HeartbeatFailed(String simulationFunctionName, String nodeIdentifier) {
		this.simulationFunctionName = simulationFunctionName;
		this.nodeIdentifier = nodeIdentifier;
	}
	
	public String getSimulationFunctionName() {
		return simulationFunctionName;
	}
	
	public String getNodeIdentifier() {
		return nodeIdentifier;
	}
}

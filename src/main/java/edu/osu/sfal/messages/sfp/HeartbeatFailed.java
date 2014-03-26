package edu.osu.sfal.messages.sfp;

import edu.osu.sfal.util.SfpName;
import edu.osu.sfal.util.SimulationFunctionName;

public class HeartbeatFailed extends SfpStatusMessage {
	
	public HeartbeatFailed(SimulationFunctionName simulationFunctionName, SfpName sfpName) {
		super(simulationFunctionName, sfpName);
	}
}

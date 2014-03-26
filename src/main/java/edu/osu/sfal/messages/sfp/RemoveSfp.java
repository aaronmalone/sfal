package edu.osu.sfal.messages.sfp;

import edu.osu.sfal.util.SfpName;
import edu.osu.sfal.util.SimulationFunctionName;

public class RemoveSfp extends SfpStatusMessage {
	public RemoveSfp(SimulationFunctionName simulationFunctionName, SfpName sfpName) {
		super(simulationFunctionName, sfpName);
	}
}

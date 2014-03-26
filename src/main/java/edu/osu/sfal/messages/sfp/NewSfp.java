package edu.osu.sfal.messages.sfp;

import edu.osu.sfal.util.SfpName;
import edu.osu.sfal.util.SimulationFunctionName;

public class NewSfp extends SfpStatusMessage {

	public NewSfp(SimulationFunctionName simulationFunctionName, SfpName sfpName) {
		super(simulationFunctionName, sfpName);
	}
}

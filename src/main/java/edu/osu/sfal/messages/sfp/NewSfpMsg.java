package edu.osu.sfal.messages.sfp;

import edu.osu.sfal.util.SfpName;
import edu.osu.sfal.util.SimulationFunctionName;

public class NewSfpMsg extends SfpStatusMessage {

	public NewSfpMsg(SimulationFunctionName simulationFunctionName, SfpName sfpName) {
		super(simulationFunctionName, sfpName);
	}
}

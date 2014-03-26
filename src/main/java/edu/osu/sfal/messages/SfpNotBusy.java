package edu.osu.sfal.messages;

import edu.osu.sfal.messages.sfp.SfpStatusMessage;
import edu.osu.sfal.util.SfpName;
import edu.osu.sfal.util.SimulationFunctionName;

public class SfpNotBusy extends SfpStatusMessage {
	public SfpNotBusy(SimulationFunctionName simulationFunctionName, SfpName sfpName) {
		super(simulationFunctionName, sfpName);
	}
}

package edu.osu.sfal.messages;

import edu.osu.sfal.util.SfpName;

public class SfpNotBusy {
	private final SfpName sfpName;

	public SfpNotBusy(SfpName sfpName) {
		this.sfpName = sfpName;
	}

	public SfpName getSfpName() {
		return sfpName;
	}
}

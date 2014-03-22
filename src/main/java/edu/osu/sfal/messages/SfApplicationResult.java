package edu.osu.sfal.messages;

import edu.osu.sfal.util.SfpName;

import java.util.Map;

public class SfApplicationResult {
	private final SfApplication sfApplication;
	private final Map<String, Object> outputs;
    private final SfpName sfpName;
	
	public SfApplicationResult(
			SfApplication simulationFunctionApplication,
            SfpName sfpName,
			Map<String, Object> outputs) {
		this.sfApplication = simulationFunctionApplication;
		this.outputs = outputs;
        this.sfpName = sfpName;
	}

	public SfApplication getSfApplication() {
		return sfApplication;
	}

    public SfpName getSfpName() { return sfpName; }

	public Map<String, Object> getOutputs() {
		return outputs;
	}
}

package edu.osu.sfal.rest;

import edu.osu.sfal.messages.SfApplicationRequest;

import java.util.Map;

/**
 * An object that holds both a request and that output mappings that should be used when the request is completed.
 *
 * An SfApplicationRequest object has the output names, but does not have the keys under which the outputs should be
 * stored in the database. This object stores that those keys as the values in the outputMappings field.
 */
public class RequestAndOutputMappings {

	private final SfApplicationRequest sfApplicationRequest;

	/** Map of output name (LAPIS variable name) to data store key under which the data should be saved */
	private final Map<String, String> outputMappings;

	public RequestAndOutputMappings(SfApplicationRequest sfApplicationRequest, Map<String, String> outputMappings) {
		this.sfApplicationRequest = sfApplicationRequest;
		this.outputMappings = outputMappings;
	}

	public SfApplicationRequest getSfApplicationRequest() {
		return sfApplicationRequest;
	}

	public Map<String, String> getOutputMappings() {
		return outputMappings;
	}
}

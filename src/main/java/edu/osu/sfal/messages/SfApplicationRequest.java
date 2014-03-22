package edu.osu.sfal.messages;

import scala.concurrent.Promise;

public class SfApplicationRequest {
	private final SfApplication sfApplication;
	private final Promise<SfApplicationResult> sfApplicationResultPromise;
	
	public SfApplicationRequest(
			SfApplication sfApplication,
			Promise<SfApplicationResult> sfApplicationResultPromise) {
		this.sfApplication = sfApplication;
		this.sfApplicationResultPromise = sfApplicationResultPromise;
	}

	public SfApplication getSfApplication() {
		return sfApplication;
	}

	public Promise<SfApplicationResult> getSfApplicationResultPromise() {
		return sfApplicationResultPromise;
	}
}

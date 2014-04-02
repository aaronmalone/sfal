package edu.osu.sfal.actors;

import edu.osu.lapis.LapisApi;
import edu.osu.sfal.messages.SfApplicationRequest;
import edu.osu.sfal.util.SfpName;
import edu.osu.sfal.util.SimulationFunctionName;

public class SfpActor extends LastMessageReceivedActor {

	private static Object
			HEARTBEAT_MSG = "HEARTBEAT_MSG",
			CHECK_ON_CALCULATION = "CHECK_ON_CALCULATION";


	private final SimulationFunctionName simulationFunctionName;
	private final SfpName sfpName;
	private final LapisApi lapisApi;

	public SfpActor(SimulationFunctionName simulationFunctionName, SfpName sfpName, LapisApi lapisApi) {
		this.simulationFunctionName = simulationFunctionName;
		this.lapisApi = lapisApi;
		this.sfpName = sfpName;
	}

	@Override public void onReceiveImpl(Object message) throws Exception {
		if(message instanceof SfApplicationRequest) {
			handleSfApplicationRequest((SfApplicationRequest) message);
		} else if(message == CHECK_ON_CALCULATION) {
			checkOnCalculation();
		} else if(message == HEARTBEAT_MSG) {
			doHeartbeatCheck();
		} else {
			unhandled(message);
		}
	}

	private void handleSfApplicationRequest(SfApplicationRequest request) {
		//TODO IMPLEMENT
	}

	private void checkOnCalculation() {
		//TODO IMPLEMENT
	}

	private void doHeartbeatCheck() {
		//todo implement
	}

	private String getNodeName() {
		return sfpName.getName();
	}
}

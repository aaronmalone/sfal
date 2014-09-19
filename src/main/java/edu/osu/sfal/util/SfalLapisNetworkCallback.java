package edu.osu.sfal.util;

import edu.osu.lapis.LapisApi;
import edu.osu.lapis.network.LapisNode;
import edu.osu.lapis.network.NetworkChangeCallback;
import edu.osu.sfal.messages.sfp.NewSfpMsg;
import edu.osu.sfal.messages.sfp.RemoveSfp;
import edu.osu.sfal.messages.sfp.SfpStatusMessage;
import org.apache.log4j.Logger;

import java.util.concurrent.TimeoutException;

public class SfalLapisNetworkCallback implements NetworkChangeCallback {

	private static class SfpInformation {
		String nodeName;
		SimulationFunctionName simulationFunctionName;
		SfpName sfpName;
	}

	static final String SIMULATION_FUNCTION_NAME = "SIMULATION_FUNCTION_NAME";

	private final Logger logger = Logger.getLogger(getClass());

	private final LapisApi lapisApi;
	private final long waitForNodeMillis;
	private final MessageDispatcher<SfpStatusMessage> messageDispatcher;

	public SfalLapisNetworkCallback(LapisApi lapisApi,
			long waitForNodeMillis,
			MessageDispatcher<SfpStatusMessage> messageDispatcher) {
		this.waitForNodeMillis = waitForNodeMillis;
		this.lapisApi = lapisApi;
		this.messageDispatcher = messageDispatcher;
	}

	@Override
	public void onNodeAdd(LapisNode lapisNode) {
		logger.info("New node on network: " + lapisNode);
		waitForNode(lapisNode);
		SfpInformation info = getSfpInformation(lapisNode);
		NewSfpMsg newSfp = new NewSfpMsg(info.simulationFunctionName, info.sfpName);
		messageDispatcher.dispatch(newSfp);
	}

	@Override
	public void onNodeDelete(LapisNode lapisNode) {
		logger.info("Node deleted from network: " + lapisNode);
		waitForNode(lapisNode);
		SfpInformation info = getSfpInformation(lapisNode);
		RemoveSfp removeSfp = new RemoveSfp(info.simulationFunctionName, info.sfpName);
		messageDispatcher.dispatch(removeSfp);
	}

	private void waitForNode(LapisNode lapisNode) {
		String nodeName = lapisNode.getNodeName();
		try {
			lapisApi.waitForReadyNode(nodeName, waitForNodeMillis);
		} catch (TimeoutException e) {
			logger.warn("Timed out while waiting for node " + lapisNode
					+ " to become ready. Waited for " + waitForNodeMillis + " millis.", e);
			throw new RuntimeException(e);
		}
	}

	private SfpInformation getSfpInformation(LapisNode lapisNode) {
		SfpInformation info = new SfpInformation();
		info.nodeName = lapisNode.getNodeName();
		info.simulationFunctionName =
				new SimulationFunctionName(lapisApi.getString(info.nodeName, SIMULATION_FUNCTION_NAME));
		info.sfpName = getSfpName(info.simulationFunctionName, lapisNode);
		return info;
	}

	private SfpName getSfpName(SimulationFunctionName sfName, LapisNode lapisNode) {
		return new SfpName(lapisNode.getNodeName());
	}

}

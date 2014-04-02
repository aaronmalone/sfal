package edu.osu.sfal.util;

import com.google.common.base.Joiner;
import edu.osu.lapis.LapisApi;
import edu.osu.lapis.network.LapisNode;
import edu.osu.lapis.network.NetworkChangeCallback;
import edu.osu.sfal.messages.sfp.NewSfp;
import edu.osu.sfal.messages.sfp.RemoveSfp;
import edu.osu.sfal.messages.sfp.SfpStatusMessage;
import org.apache.log4j.Logger;

import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public class SfalLapisNetworkCallback implements NetworkChangeCallback {

	private static class SfpInformation {
		String nodeName;
		SimulationFunctionName simulationFunctionName;
		SfpName sfpName;
	}

	//TODO MOVE TO CONSTANTS
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
		NewSfp newSfp = new NewSfp(info.simulationFunctionName, info.sfpName);
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
					+ " to become ready.", e);
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
		String s = Joiner.on('+').join(sfName.getName(), lapisNode.getNodeName(), lapisNode.getUrl());
		return new SfpName(s);
	}

}

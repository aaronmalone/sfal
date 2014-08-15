package edu.osu.sfal.util;

import edu.osu.lapis.LapisApi;
import edu.osu.lapis.network.LapisNode;
import edu.osu.sfal.messages.sfp.NewSfpMsg;
import edu.osu.sfal.messages.sfp.RemoveSfp;
import edu.osu.sfal.messages.sfp.SfpStatusMessage;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static edu.osu.sfal.util.SfalLapisNetworkCallback.SIMULATION_FUNCTION_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SfalLapisNetworkCallbackTest {

	private final long WAIT = 15;

	private final AtomicReference<SfpStatusMessage> messageReference = new AtomicReference<>();
	private final MessageDispatcher<SfpStatusMessage> messageDispatcher = msg -> messageReference.set(msg);

	private String nodeName;
	private String simulationFunctionName;
	private String url;
	private SfalLapisNetworkCallback callback;

	@Before
	public void setUp() {
		nodeName = RandomStringUtils.randomAlphabetic(16);
		simulationFunctionName = RandomStringUtils.randomAlphabetic(16);
		url = "http://" + RandomStringUtils.randomAlphanumeric(50);

		LapisApi lapisApi = getLapisApi(nodeName, simulationFunctionName);
		callback = new SfalLapisNetworkCallback(lapisApi, WAIT, messageDispatcher);
	}

	private LapisApi getLapisApi(String node, String simFun) {
		LapisApi lapisApi = mock(LapisApi.class);
		when(lapisApi.getString(node, SIMULATION_FUNCTION_NAME)).thenReturn(simFun);
		return lapisApi;
	}

	@After
	public void clearReference() {
		messageReference.set(null);
	}

	@Test
	public void testNodeAdd() throws TimeoutException {
		assertTrue(messageReference.get() == null);
		callback.onNodeAdd(new LapisNode(nodeName, url));
		assertTrue(messageReference.get() != null);
		assertTrue(messageReference.get() instanceof NewSfpMsg);
		SfpStatusMessage sfpStatus = (SfpStatusMessage) messageReference.get();
		commonAssertions(sfpStatus);
	}

	@Test
	public void testNodeDelete() {
		assertTrue(messageReference.get() == null);
		callback.onNodeDelete(new LapisNode(nodeName, url));
		assertTrue(messageReference.get() != null);
		assertTrue(messageReference.get() instanceof RemoveSfp);
		SfpStatusMessage sfpStatus = (SfpStatusMessage) messageReference.get();
		commonAssertions(sfpStatus);
	}

	;

	private void commonAssertions(SfpStatusMessage sfpStatus) {
		SimulationFunctionName simFunNameInMsg = sfpStatus.getSimulationFunctionName();
		SfpName sfpNameInMsg = sfpStatus.getSfpName();
		assertEquals(new SfpName(nodeName), sfpNameInMsg);
		assertEquals(new SimulationFunctionName(simulationFunctionName), simFunNameInMsg);
	}
}

package edu.osu.sfal.actors;

import akka.actor.ActorRef;
import com.google.common.annotations.VisibleForTesting;
import edu.osu.lapis.Flags;
import edu.osu.lapis.LapisApi;
import edu.osu.sfal.messages.SfApplicationRequest;
import edu.osu.sfal.messages.SfApplicationResult;
import edu.osu.sfal.messages.sfp.HeartbeatFailed;
import edu.osu.sfal.util.SfpName;
import edu.osu.sfal.util.SimulationFunctionName;
import org.apache.commons.lang3.Validate;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.FiniteDuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class SfpActor extends LastMessageReceivedActor {

	static String
			READY_TO_CALCULATE_VAR_NAME = "readyToCalculate",
			FINISHED_CALCULATING_VAR_NAME = "finishedCalculating";

	static Object
			HEARTBEAT_MSG = "HEARTBEAT_MSG",
			CHECK_ON_CALCULATION = "CHECK_ON_CALCULATION";


	private final SimulationFunctionName simulationFunctionName;
	private final SfpName sfpName;
	private final LapisApi lapisApi;

	private final ActorRef checkOnCalcDestination;
	private final ActorRef heartbeatCheckDestination;
	private final ActorRef heartbeatFailedDestination;

	private SfApplicationRequest currentRequest = null;

	public SfpActor(SimulationFunctionName simulationFunctionName, SfpName sfpName, LapisApi lapisApi) {
		this.simulationFunctionName = simulationFunctionName;
		this.lapisApi = lapisApi;
		this.sfpName = sfpName;
		this.checkOnCalcDestination = getSelf();
		this.heartbeatCheckDestination = getSelf();
		this.heartbeatFailedDestination = getContext().parent();
//		scheduleHeartbeatCheck();
	}

	/* it looks like there's an opportunity for DRY here but there's not -- trust me */

	@VisibleForTesting SfpActor(SimulationFunctionName simulationFunctionName, SfpName sfpName, LapisApi lapisApi,
			ActorRef checkOnCalcDestination, ActorRef heartbeatCheckDestination, ActorRef heartbeatFailedDestination) {
		this.simulationFunctionName = simulationFunctionName;
		this.lapisApi = lapisApi;
		this.sfpName = sfpName;
		this.checkOnCalcDestination = checkOnCalcDestination;
		this.heartbeatCheckDestination = heartbeatCheckDestination;
		this.heartbeatFailedDestination = heartbeatFailedDestination;
		scheduleHeartbeatCheck();
	}

	private void scheduleHeartbeatCheck() {
		scheduleOnce(15 /*TODO MAKE CONFIGURABLE*/, heartbeatCheckDestination, HEARTBEAT_MSG);
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
		validateSfpNonCurrentlyCalculating();
		setCurrentRequest(request);
		setInputVariablesOnSfp(request.getInputs());
		setReadyToCalculateFlag();
		scheduleCheckOnCalculation();
	}

	private void validateSfpNonCurrentlyCalculating() {
		boolean readyToCalculateFlag = getFlag(READY_TO_CALCULATE_VAR_NAME);
		boolean finishedCalculatingFlag = getFlag(FINISHED_CALCULATING_VAR_NAME);
		Validate.isTrue(!readyToCalculateFlag);
		Validate.isTrue(finishedCalculatingFlag);
	}

	public void setCurrentRequest(SfApplicationRequest currentRequest) {
		Validate.isTrue(this.currentRequest == null,
				"current request should be null before receiving new request");
		this.currentRequest = currentRequest;
	}

	private void setInputVariablesOnSfp(Map<String, Object> inputs) {
		inputs.forEach((name, value) -> lapisApi.set(getNodeName(), name, value));
	}

	private void setReadyToCalculateFlag() {
		lapisApi.set(getNodeName(), READY_TO_CALCULATE_VAR_NAME, Flags.FLAG_VALUE_TRUE);
	}

	private void scheduleCheckOnCalculation() {
		long delayMillis = 50; /*TODO MAKE CONFIGURABLE*/
		scheduleOnce(delayMillis, checkOnCalcDestination, CHECK_ON_CALCULATION);
	}

	private void checkOnCalculation() {
		boolean finishedCalculating = getFlag(FINISHED_CALCULATING_VAR_NAME);
		if(finishedCalculating) {
			handleFinishedCalculation();
		} else {
			scheduleCheckOnCalculation();
		}
	}

	private void handleFinishedCalculation() {
		Map<String, Object> outputValues = getOutputValuesFromCurrentCalculation();
		SfApplicationResult result = createsSfApplicationResultForCurrentCalculation(outputValues);
		completeAndClearCurrentRequest(result);
	}

	private Map<String, Object> getOutputValuesFromCurrentCalculation() {
		Set<String> outputNames = currentRequest.getOutputNames();
		Map<String, Object> outputValuesMap = new HashMap<>();
		for (String outputName : outputNames) {
			Object value = lapisApi.getObject(getNodeName(), outputName);
			outputValuesMap.put(outputName, value);
		}
		return outputValuesMap;
	}

	private SfApplicationResult createsSfApplicationResultForCurrentCalculation(Map<String, Object> outputValues) {
		return new SfApplicationResult(simulationFunctionName, currentRequest.getTimestep(), outputValues, sfpName);
	}

	private void completeAndClearCurrentRequest(SfApplicationResult result) {
		currentRequest.getCompletableFuture().complete(result);
		currentRequest = null;
	}

	private void doHeartbeatCheck() {
		boolean nodeIsLive = lapisApi.doHeartbeatCheckReturnNodeIsLive(getNodeName());
		if(nodeIsLive) {
			scheduleHeartbeatCheck();
		} else {
			handleNotNodeAlive();
		}
	}

	private void handleNotNodeAlive() {
		Object heartbeatFailed = new HeartbeatFailed(simulationFunctionName, sfpName);
		heartbeatFailedDestination.tell(heartbeatFailed, getSelf());
		if(currentRequest != null) {
			Throwable exception = new IllegalStateException("SFP failed heartbeat while processing request.");
			currentRequest.getCompletableFuture().completeExceptionally(exception);
		}
		getContext().stop(getSelf());
	}

	private void scheduleOnce(long delayMillis, ActorRef destination, Object message) {
		FiniteDuration delay = FiniteDuration.create(delayMillis, TimeUnit.MILLISECONDS);
		ExecutionContext executionContext = getContext().dispatcher();
		ActorRef sender = getSelf();
		getContext().system().scheduler()
				.scheduleOnce(delay, destination, message, executionContext, sender);
	}

	private boolean getFlag(String flagName) {
		double[] flag = lapisApi.getArrayOfDouble(getNodeName(), flagName);
		return Flags.evaluateFlagValue(flag);
	}

	private String getNodeName() {
		return sfpName.getName();
	}

	@VisibleForTesting SfApplicationRequest getCurrentRequest() {
		return currentRequest;
	}
}

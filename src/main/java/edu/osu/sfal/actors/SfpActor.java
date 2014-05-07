package edu.osu.sfal.actors;

import akka.actor.ActorRef;
import akka.actor.SupervisorStrategy;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import edu.osu.lapis.Flags;
import edu.osu.lapis.LapisApi;
import edu.osu.sfal.messages.SfApplicationRequest;
import edu.osu.sfal.messages.SfApplicationResult;
import edu.osu.sfal.messages.SfpNotBusy;
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

public class SfpActor extends UntypedActor {

	private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

	static String
			READY_TO_CALCULATE_VAR_NAME = "readyToCalculate",
			FINISHED_CALCULATING_VAR_NAME = "finishedCalculating";

	static Object
			HEARTBEAT_MSG = "HEARTBEAT_MSG",
			CHECK_ON_CALCULATION = "CHECK_ON_CALCULATION";

	private static long heartbeatPeriodMillis = 15000;
	private static long calculationCheckPeriodMillis = 1000;

	private final SimulationFunctionName simulationFunctionName;
	private final SfpName sfpName;
	private final LapisApi lapisApi;

	private final ActorRef checkOnCalcDestination;
	private final ActorRef heartbeatCheckDestination;
	private final ActorRef heartbeatFailedDestination;

	private SfApplicationRequest currentRequest = null;

	public SfpActor(SimulationFunctionName simulationFunctionName, SfpName sfpName, LapisApi lapisApi) {
		this(simulationFunctionName, sfpName, lapisApi, null);
	}

	@VisibleForTesting SfpActor(SimulationFunctionName simFunName, SfpName sfp, LapisApi lapisApi, ActorRef ref) {
		this.simulationFunctionName = simFunName;
		this.lapisApi = lapisApi;
		this.sfpName = sfp;
		this.checkOnCalcDestination = Objects.firstNonNull(ref, getSelf());
		this.heartbeatCheckDestination = Objects.firstNonNull(ref, getSelf());
		this.heartbeatFailedDestination = Objects.firstNonNull(ref, getContext().parent());
		scheduleHeartbeatCheck();
	}

	private void scheduleHeartbeatCheck() {
		scheduleOnce(heartbeatPeriodMillis, heartbeatCheckDestination, HEARTBEAT_MSG);
	}

	@Override public void onReceive(Object message) throws Exception {
		logger.debug("Received message {} of type {}", message, message.getClass().getSimpleName());
		logger.debug("Current thread: {}", Thread.currentThread());
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
		logger.info("Handling new SfApplicationRequest: {}", request);
		validateSfpNonCurrentlyCalculating();
		setCurrentRequest(request);
		setInputVariablesAndTimeStep(request.getInputs(), request.getTimestep());
		setReadyToCalculateFlag();
		scheduleCheckOnCalculation();
	}

	private void validateSfpNonCurrentlyCalculating() {
		boolean readyToCalculateFlag = getFlag(READY_TO_CALCULATE_VAR_NAME);
		boolean finishedCalculatingFlag = getFlag(FINISHED_CALCULATING_VAR_NAME);
		String whenMessage = " when we tried to begin setting inputs";
		Validate.isTrue(!readyToCalculateFlag, "'readyToCalculate' was set" + whenMessage);
		Validate.isTrue(finishedCalculatingFlag, "'finishedCalculating' was not set" + whenMessage);
	}

	public void setCurrentRequest(SfApplicationRequest currentRequest) {
		Validate.isTrue(this.currentRequest == null,
				"current request should be null before receiving new request");
		this.currentRequest = currentRequest;
	}

	private void setInputVariablesAndTimeStep(Map<String, Object> inputs, int timestep) {
		inputs.forEach(this::setIndividualInputVariable);
		setIndividualInputVariable("timestep", new int[]{timestep});
	}

	private void setIndividualInputVariable(String name, Object value) {
		try {
			logger.debug("Setting input variable '{}' on node '{}'", name, getNodeName());
			lapisApi.set(getNodeName(), name, value);
		} catch(Exception e) {
			throw new RuntimeException("Exception while setting variable '"
					+ name + "' on node '" + getNodeName() + "'", e);
		}
	}

	private void setReadyToCalculateFlag() {
		logger.debug("Setting flag '{}' on node '{}'", READY_TO_CALCULATE_VAR_NAME, getNodeName());
		lapisApi.set(getNodeName(), READY_TO_CALCULATE_VAR_NAME, Flags.FLAG_VALUE_TRUE);
	}

	private void scheduleCheckOnCalculation() {
		scheduleOnce(calculationCheckPeriodMillis, checkOnCalcDestination, CHECK_ON_CALCULATION);
	}

	private void checkOnCalculation() {
		boolean finishedCalculating = getFlag(FINISHED_CALCULATING_VAR_NAME);
		if(finishedCalculating && !getFlag(READY_TO_CALCULATE_VAR_NAME)) {
			handleFinishedCalculation();
		} else {
			scheduleCheckOnCalculation();
		}
	}

	private void handleFinishedCalculation() {
		logger.info("Handling completion of calculation for current request: {}", currentRequest);
		Map<String, Object> outputValues = getOutputValuesFromCurrentCalculation();
		SfApplicationResult result = createsSfApplicationResultForCurrentCalculation(outputValues);
		completeAndClearCurrentRequest(result);
		sendNotBusyMessage();
	}

	private Map<String, Object> getOutputValuesFromCurrentCalculation() {
		Set<String> outputNames = currentRequest.getOutputNames();
		Map<String, Object> outputValuesMap = new HashMap<>();
		for (String outputName : outputNames) {
			Object value = getSingleOutputValue(outputName);
			outputValuesMap.put(outputName, value);
		}
		return outputValuesMap;
	}

	private Object getSingleOutputValue(String outputName) {
		logger.debug("Retrieving output value '{}'", outputName);
		return lapisApi.getObject(getNodeName(), outputName);
	}

	private SfApplicationResult createsSfApplicationResultForCurrentCalculation(Map<String, Object> outputValues) {
		return new SfApplicationResult(simulationFunctionName, currentRequest.getTimestep(), outputValues, sfpName);
	}

	private void completeAndClearCurrentRequest(SfApplicationResult result) {
		currentRequest.getCompletableFuture().complete(result);
		logger.debug("Successfully completed request: {}", currentRequest);
		currentRequest = null;
	}

	private void doHeartbeatCheck() {
		boolean nodeIsLive = lapisApi.doHeartbeatCheckReturnNodeIsLive(getNodeName());
		if(nodeIsLive) {
			scheduleHeartbeatCheck();
		} else {
			handleNodeFailedHeartbeatCheck();
		}
	}

	private void handleNodeFailedHeartbeatCheck() {
		logger.warning("Node '{}' failed heartbeat check.", getNodeName());
		Object heartbeatFailed = new HeartbeatFailed(simulationFunctionName, sfpName);
		heartbeatFailedDestination.tell(heartbeatFailed, getSelf());
		if(currentRequest != null) {
			Throwable exception = new IllegalStateException("SFP failed heartbeat while processing request.");
			logger.warning("Completing current request {} with exception: {}", currentRequest, exception);
			currentRequest.getCompletableFuture().completeExceptionally(exception);
		}
		logger.debug("Shutting down actor {}", getSelf());
		getContext().stop(getSelf());
		//TODO MAYBE THROW EXCEPTION
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

	private void sendNotBusyMessage() {
		logger.debug("Sending not-busy message.");
		SfpNotBusy sfpNotBusy = new SfpNotBusy(simulationFunctionName, sfpName);
		this.getContext().parent().tell(sfpNotBusy, getSelf());
	}

	@Override
	public void postRestart(Throwable reason) throws Exception {
		sendNotBusyMessage();
		super.postRestart(reason);
	}

	@VisibleForTesting SfApplicationRequest getCurrentRequest() {
		return currentRequest;
	}

	@VisibleForTesting static void setHeartbeatPeriodMillis(long heartbeatPeriodMillis) {
		SfpActor.heartbeatPeriodMillis = heartbeatPeriodMillis;
	}

	@VisibleForTesting static void setCalculationCheckPeriodMillis(long calculationCheckPeriodMillis) {
		SfpActor.calculationCheckPeriodMillis = calculationCheckPeriodMillis;
	}
}

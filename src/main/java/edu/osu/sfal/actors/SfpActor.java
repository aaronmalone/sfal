package edu.osu.sfal.actors;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.google.common.annotations.VisibleForTesting;
import edu.osu.lapis.Flags;
import edu.osu.lapis.LapisApi;
import edu.osu.sfal.data.OutputValuesMap;
import edu.osu.sfal.messages.SfApplicationRequest;
import edu.osu.sfal.messages.SfApplicationResult;
import edu.osu.sfal.messages.SfpNotBusy;
import edu.osu.sfal.messages.sfp.HeartbeatFailedMsg;
import edu.osu.sfal.util.SfpName;
import edu.osu.sfal.util.SimulationFunctionName;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.FiniteDuration;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static edu.osu.sfal.messages.Messages.CHECK_ON_CALCULATION;
import static edu.osu.sfal.messages.Messages.HEARTBEAT_MSG;
import static java.util.stream.Collectors.toMap;

/**
 * This actor handles interactions with a single SFP.
 */
public class SfpActor extends UntypedActor {

	private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

	public static final String READY_TO_CALCULATE_VAR_NAME = "readyToCalculate";
	public static final String FINISHED_CALCULATING_VAR_NAME = "finishedCalculating";

	private static long heartbeatPeriodMillis = 15000;
	private static long calculationCheckPeriodMillis = 4900;

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
	}

	/**
	 * Constructs an instance of SfpActor that sends all outbound Akka messages to the given ActorRef.
	 * This constructor is used in tests.
	 */
	@VisibleForTesting
	SfpActor(SimulationFunctionName simFunName, SfpName sfp, LapisApi lapisApi, ActorRef ref) {
		this.simulationFunctionName = simFunName;
		this.lapisApi = lapisApi;
		this.sfpName = sfp;
		this.checkOnCalcDestination = ref;
		this.heartbeatCheckDestination = ref;
		this.heartbeatFailedDestination = ref;
		scheduleHeartbeatCheck();
	}

	private void scheduleHeartbeatCheck() {
		scheduleOnce(heartbeatPeriodMillis, heartbeatCheckDestination, HEARTBEAT_MSG);
	}

	@Override
	public void onReceive(Object message) throws Exception {
		logger.debug("Received message {} of type {}", message, message.getClass().getSimpleName());
		logger.debug("Current thread: {}", Thread.currentThread());
		if (message instanceof SfApplicationRequest) {
			handleSfApplicationRequest((SfApplicationRequest) message);
		} else if (message == CHECK_ON_CALCULATION) {
			checkOnCalculation();
		} else if (message == HEARTBEAT_MSG) {
			doHeartbeatCheck();
		} else {
			unhandled(message);
		}
	}

	private void handleSfApplicationRequest(SfApplicationRequest request) {
		logger.info("Handling new SfApplicationRequest: {}", request);
		validateSfpNotCurrentlyCalculating();
		setCurrentRequest(request);
		setInputVariablesAndTimeStep();
		setReadyToCalculateFlag();
		scheduleCheckOnCalculation();
	}

	private void validateSfpNotCurrentlyCalculating() {
		if (getFinishedCalculatingFlag() && !getReadyToCalculateFlag()) {
			logger.debug("Node '{}' is not currently calculating ", getNodeName());
		} else {
			throw new IllegalStateException("Node " + getNodeName() + " appears to be running a calculation.");
		}
	}

	public void setCurrentRequest(SfApplicationRequest currentRequest) {
		if(this.currentRequest == null) {
			this.currentRequest = currentRequest;
		} else {
			throw new IllegalStateException("current request should be null before processing new request");
		}
	}

	private void setInputVariablesAndTimeStep() {
		assert this.currentRequest != null;
		int timestep = this.currentRequest.getTimestep();
		setVariableOnNode("timestep", new int[]{timestep});
		this.currentRequest.getInputs().forEach(this::setVariableOnNode);
	}

	private void setReadyToCalculateFlag() {
		setVariableOnNode(READY_TO_CALCULATE_VAR_NAME, Flags.getFlagTrue());
	}

	private void setVariableOnNode(String variableName, Object value) {
		try {
			logger.debug("Setting variable '{}' on node '{}'", variableName, getNodeName());
			lapisApi.set(getNodeName(), variableName, value);
		} catch (Exception e) {
			throw new RuntimeException("Exception while setting variable '"
					+ variableName + "' on node '" + getNodeName() + "'", e);
		}
	}

	private void scheduleCheckOnCalculation() {
		scheduleOnce(calculationCheckPeriodMillis, checkOnCalcDestination, CHECK_ON_CALCULATION);
	}

	private void checkOnCalculation() {
		if(getFinishedCalculatingFlag() && !getReadyToCalculateFlag()) {
			handleFinishedCalculation();
		} else {
			scheduleCheckOnCalculation();
		}
	}

	/**
	 * Handles processing when a calculation finishes. This method should get the
	 * output data from the SFP, create an SfApplicationResult object, set the
	 * value of the CompletableFuture associated with the current request, clear
	 * the current request, and send a message to the SfpPoolManager.
	 */
	private void handleFinishedCalculation() {
		logger.debug("Handling completion of calculation for current request: {}", currentRequest);
		OutputValuesMap outputValues = getOutputValuesFromCurrentCalculation();
		SfApplicationResult result = createsSfApplicationResultForCurrentCalculation(outputValues);
		completeAndClearCurrentRequest(result);
		sendNotBusyMessage();
	}

	/**
	 * Retrieves the output data from the SFP for the current calculation.
	 * Returns a map from the published LAPIS variable names to the
	 * corresponding values.
	 */
	private OutputValuesMap getOutputValuesFromCurrentCalculation() {
		Map<String, Object> map = currentRequest
				.getOutputNames()
				.stream()
				.collect(toMap(name -> name, this::getSingleOutputValue));
		return OutputValuesMap.fromMap(map);
	}

	/**
	 * Gets the value of single published variable.
	 */
	private Object getSingleOutputValue(String outputVariableName) {
		logger.debug("Retrieving output value '{}'", outputVariableName);
		Object value = lapisApi.getObject(getNodeName(), outputVariableName);
		if (value == null) {
			logger.warning("Value for '{}' was null.", outputVariableName);
		}
		return value;
	}

	private SfApplicationResult createsSfApplicationResultForCurrentCalculation(OutputValuesMap outputValues) {
		return new SfApplicationResult(simulationFunctionName, currentRequest.getTimestep(), outputValues, sfpName);
	}

	private void completeAndClearCurrentRequest(SfApplicationResult result) {
		currentRequest.getCompletableFuture().complete(result);
		logger.info("Successfully completed request: {}", currentRequest);
		currentRequest = null;
	}

	/**
	 * Sends a message to the parent (SfpPoolManager) indicating that this
	 * SFP is not busy (not currently working on a calculation).
	 */
	private void sendNotBusyMessage() {
		logger.debug("Sending not-busy message.");
		SfpNotBusy sfpNotBusy = new SfpNotBusy(simulationFunctionName, sfpName);
		this.getContext().parent().tell(sfpNotBusy, getSelf());
	}

	private void doHeartbeatCheck() {
		logger.debug("About to do heartbeat check for node '{}'...", getNodeName());
		boolean nodeIsLive = lapisApi.doHeartbeatCheckReturnNodeIsLive(getNodeName());
		if (nodeIsLive) {
			scheduleHeartbeatCheck();
		} else {
			handleNodeFailedHeartbeatCheck();
		}
	}

	private void handleNodeFailedHeartbeatCheck() {
		logger.warning("Node '{}' failed heartbeat check.", getNodeName());
		Object heartbeatFailed = new HeartbeatFailedMsg(simulationFunctionName, sfpName);
		heartbeatFailedDestination.tell(heartbeatFailed, getSelf());
		if (currentRequest != null) {
			Throwable exception = new IllegalStateException("SFP failed heartbeat while processing request.");
			logger.warning("Completing current request {} with exception: {}", currentRequest, exception);
			currentRequest.getCompletableFuture().completeExceptionally(exception);
		}
		logger.debug("Shutting down actor {}", getSelf());
		getContext().stop(getSelf());
	}

	private void scheduleOnce(long delayMillis, ActorRef destination, Object message) {
		FiniteDuration delay = FiniteDuration.create(delayMillis, TimeUnit.MILLISECONDS);
		ExecutionContext executionContext = getContext().dispatcher();
		ActorRef sender = getSelf();
		getContext().system().scheduler()
				.scheduleOnce(delay, destination, message, executionContext, sender);
	}

	private boolean getFinishedCalculatingFlag() {
		return getFlag(FINISHED_CALCULATING_VAR_NAME);
	}

	private boolean getReadyToCalculateFlag() {
		return getFlag(READY_TO_CALCULATE_VAR_NAME);
	}

	/**
	 * Uses a LAPIS get operation to get a flag-style variable and returns the
	 * equivalent boolean value.
	 *
	 * @param flagName the name of the flag
	 * @return the corresponding boolean value
	 */
	private boolean getFlag(String flagName) {
		double[] flag = lapisApi.getArrayOfDouble(getNodeName(), flagName);
		return Flags.evaluateFlagValue(flag);
	}

	/**
	 * Returns the SFP name.
	 */
	private String getNodeName() {
		return sfpName.getName();
	}

	@VisibleForTesting
	SfApplicationRequest getCurrentRequest() {
		return currentRequest;
	}

	@VisibleForTesting
	static void setHeartbeatPeriodMillis(long heartbeatPeriodMillis) {
		SfpActor.heartbeatPeriodMillis = heartbeatPeriodMillis;
	}

	@VisibleForTesting
	static void setCalculationCheckPeriodMillis(long calculationCheckPeriodMillis) {
		SfpActor.calculationCheckPeriodMillis = calculationCheckPeriodMillis;
	}
}

package edu.osu.sfal.actors;


import akka.actor.ActorRef;
import akka.event.LoggingAdapter;
import edu.osu.lapis.Flags;
import edu.osu.lapis.LapisApi;
import edu.osu.sfal.messages.SfApplicationRequest;
import edu.osu.sfal.util.SfpName;
import org.apache.commons.lang3.Validate;
import scala.concurrent.duration.FiniteDuration;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import static edu.osu.lapis.Flags.getFlagTrue;
import static java.time.Instant.now;

public class SecondSfpActor extends ActorDelegate {

	public static Object HEARTBEAT_MSG = "HEARTBEAT_MSG", //TODO MOVE THESE
			CHECK_ON_CALCULATION = "CHECK_ON_CALCULATION";

	public static final String READY_TO_CALCULATE_VAR_NAME = "readyToCalculate";
	public static final String FINISHED_CALCULATING_VAR_NAME = "finishedCalculating";

	private final ActorRef calculationCheckDestination;
	private final ScheduledExecutorService scheduledExecutorService;
	private final LapisApi lapisClient;
	private final LoggingAdapter logger;
	private final ActorRef selfActorRef;
	private final SfpName sfpName;
	private final SfalScheduler sfalScheduler;
	private final FiniteDuration checkOnCalculationDelay;
	private final Duration calculationSetupTimeout;

	private SfApplicationRequest currentSfApplicationRequest = null;
	private Instant calculationSetupTime = null;
	private Future<?> calculationSetupFuture = null;

	public SecondSfpActor(ActorRef calculationCheckDestination, ScheduledExecutorService scheduledExecutorService,
						  LapisApi lapisClient, LoggingAdapter logger, ActorRef selfActorRef, SfpName sfpName,
						  SfalScheduler sfalScheduler, FiniteDuration checkOnCalculationDelay, Duration calculationSetupTimeout) { //TODO REGENERATE
		super(selfActorRef);
		this.calculationCheckDestination = calculationCheckDestination;
		this.scheduledExecutorService = scheduledExecutorService;
		this.lapisClient = lapisClient;
		this.logger = logger;
		this.selfActorRef = selfActorRef;
		this.sfpName = sfpName;
		//TODO HEARTBEAT AND CALCULATION CHECK
		this.sfalScheduler = sfalScheduler;
		this.checkOnCalculationDelay = checkOnCalculationDelay;
		this.calculationSetupTimeout = calculationSetupTimeout;
	}

	public void handleIncomingRequest(SfApplicationRequest sfApplicationRequest) {
		logger.info("SFP actor for '{}' node received request {}", sfpName.getName(), sfApplicationRequest);
		setCurrentRequest(sfApplicationRequest);
		this.calculationSetupFuture = scheduledExecutorService.submit(() -> handleRequestInternal());
		tell(calculationCheckDestination, CHECK_ON_CALCULATION);
	}

	private void handleRequestInternal() {
		logger.debug("Handling request {}", this.currentSfApplicationRequest);
		validateSfpNotCurrentlyCalculating();
		setInputsAndTimestep();
		setLapisVariable(READY_TO_CALCULATE_VAR_NAME, getFlagTrue());
		logger.debug("Set ready-flag to begin calculation.");
		//TODO SCHEDULE CHECK ON CALCULATION??
	}

/*	private void scheduleCheckOnCalculation() {
		sfalScheduler.scheduleOnce(checkOnCalculationDelay, selfActorRef, CHECK_ON_CALCULATION); //TODO CHANGE CHECK MESSAGE OBJECT
	}*/

	private void setInputsAndTimestep() {
		int timestep = this.currentSfApplicationRequest.getTimestep();
		setLapisVariable("timestep", new int[]{timestep});
		this.currentSfApplicationRequest.getInputs().forEach(this::setLapisVariable);
	}

	private void setLapisVariable(String variableName, Object value) {
		logger.debug("Setting variable '{}' on node '{}'", variableName, getNodeName());
		lapisClient.set(getNodeName(), variableName, value);
	}

	private void setCurrentRequest(SfApplicationRequest sfApplicationRequest) {
		if (this.currentSfApplicationRequest == null) {
			this.currentSfApplicationRequest = sfApplicationRequest;
		} else {
			throw new IllegalStateException("Attempted to set current request, "
					+ "but there is an existing (non-null) request associated with this SFP");
		}
	}

	//TODO RE-ORDER MEMBERS

	private void validateSfpNotCurrentlyCalculating() {
		boolean finishedCalculatingFlag = getFlag(FINISHED_CALCULATING_VAR_NAME);
		boolean readyToCalculateFlag = getFlag(READY_TO_CALCULATE_VAR_NAME);
		if (finishedCalculatingFlag && !readyToCalculateFlag) {
			logger.debug("SFP {} not currently calculating.", sfpName.getName());
		} else {
			throw new IllegalStateException("SFP might be in the middle of a calculation.");
		}
	}

	/**
	 * Uses a LAPIS get operation to get a flag-style variable and returns the
	 * equivalent boolean value.
	 *
	 * @param flagName the name of the flag
	 * @return the corresponding boolean value
	 */
	private boolean getFlag(String flagName) {
		double[] flag = lapisClient.getArrayOfDouble(getNodeName(), flagName);
		return Flags.evaluateFlagValue(flag);
	}

	public void checkOnCalculation() {
		Validate.notNull(this.currentSfApplicationRequest);
		Validate.notNull(this.calculationSetupTime);
		Validate.notNull(this.calculationSetupFuture);
		if (this.calculationSetupFuture.isDone()) {
			boolean finished = checkFlagsToDetermineIfCalculationIsFinished();
			//TODO HANDLE FINISH
		} else if(now().isAfter(calculationSetupTime.plus(calculationSetupTimeout))) {

		}

	}

	private boolean checkFlagsToDetermineIfCalculationIsFinished() {
		boolean finishedCalculatingFlag = getFlag(FINISHED_CALCULATING_VAR_NAME);
		if (finishedCalculatingFlag && !getFlag(READY_TO_CALCULATE_VAR_NAME)) {
			logger.debug("Flags indicate calculation has finished.");
			return true;
		} else {
			logger.debug("Calculation not finished.");
			return false;
		}
	}

	/**
	 * Returns the SFP name.
	 */
	private String getNodeName() {
		return sfpName.getName();
	}
}

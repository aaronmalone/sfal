package edu.osu.sfal.actors;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.google.common.collect.Maps;
import edu.osu.lapis.LapisApi;
import edu.osu.sfal.messages.SfApplicationRequest;
import edu.osu.sfal.messages.SfpNotBusy;
import edu.osu.sfal.messages.sfp.HeartbeatFailedMsg;
import edu.osu.sfal.messages.sfp.NewSfpMsg;
import edu.osu.sfal.util.SfpName;
import edu.osu.sfal.util.SimulationFunctionName;
import org.apache.commons.lang3.Validate;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class SfpPoolManager extends UntypedActor {

	private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

	static final boolean BUSY = true, NOT_BUSY = false;

	private final SimulationFunctionName simulationFunctionName;
	private final LapisApi lapisApi;

	final Queue<SfApplicationRequest> requestQueue = new LinkedList<>();
	final Map<SfpName, ActorRef> sfpActorMap = Maps.newHashMap();
	final Map<SfpName, Boolean> sfpBusyMap = Maps.newHashMap();

	public SfpPoolManager(SimulationFunctionName simulationFunctionName, LapisApi lapisApi) {
		this.simulationFunctionName = simulationFunctionName;
		this.lapisApi = lapisApi;
	}

	@Override
	public void onReceive(Object message) throws Exception {
		logger.debug("Received message {} of type {}", message, message.getClass().getSimpleName());
		logger.debug("Current thread: {}", Thread.currentThread());
		if (message instanceof NewSfpMsg) {
			handleNewSfpRegistration((NewSfpMsg) message);
		} else if (message instanceof SfApplicationRequest) {
			handleSfApplicationRequest((SfApplicationRequest) message);
		} else if (message instanceof SfpNotBusy) {
			handleSfpNotBusy(((SfpNotBusy) message).getSfpName());
		} else if (message instanceof HeartbeatFailedMsg) {
			handleSfpHeartbeatFailed((HeartbeatFailedMsg) message);
		} else {
			unhandled(message);
		}
	}

	private void handleNewSfpRegistration(NewSfpMsg newSfp) {
		validateSimFunctionName(newSfp);
		SfpName sfpName = newSfp.getSfpName();
		ActorRef actorRef = createSfpActor(sfpName);
		sfpActorMap.put(sfpName, actorRef);
		sfpBusyMap.put(sfpName, NOT_BUSY);
		handleSfpNotBusy(sfpName);
	}

	private ActorRef createSfpActor(SfpName sfpName) {
		Props props =
				Props.create(SfpActor.class, simulationFunctionName, sfpName, lapisApi)
						.withDispatcher("pinned-dispatcher");
		ActorRef actorRef = getContext().actorOf(props);
		logger.info("Created new SfpActor: {}", actorRef);
		return actorRef;
	}

	/**
	 * Handles the simulation function application request.
	 */
	private void handleSfApplicationRequest(SfApplicationRequest sfApplicationRequest) {
		validateSimFunctionName(sfApplicationRequest);
		attemptToDispatchRequest(sfApplicationRequest);
	}

	/**
	 * Dispatches request to an SfpActor if any are not busy. If all are busy, enqueues request.
	 */
	private void attemptToDispatchRequest(SfApplicationRequest sfApplicationRequest) {
		if (isPoolEmptyOfSFPs()) {
			handlePoolEmptyOfSFPs(sfApplicationRequest);
		} else if (allPooledSFPsAreBusy()) {
			logger.debug("All SFPs busy. Queueing request {}", sfApplicationRequest);
			requestQueue.add(sfApplicationRequest);
		} else {
			dispatchRequest(sfApplicationRequest);
		}
	}

	private boolean isPoolEmptyOfSFPs() {
		return sfpActorMap.isEmpty();
	}

	private void handlePoolEmptyOfSFPs(SfApplicationRequest sfApplicationRequest) {
		throw new IllegalStateException("Received SfApplicationRequest when pool of SFPs is empty. " +
				"Request was: " + sfApplicationRequest);
	}

	private boolean allPooledSFPsAreBusy() {
		Validate.isTrue(!sfpBusyMap.isEmpty());
		for (boolean busy : sfpBusyMap.values()) {
			if (!busy) {
				return false;
			}
		}
		return true;
	}

	private void dispatchRequest(SfApplicationRequest sfApplicationRequest) {
		// may want to implement preferred order in the future.. for now we just grab the first one
		for (Map.Entry<SfpName, Boolean> entry : sfpBusyMap.entrySet()) {
			SfpName sfp = entry.getKey();
			boolean busy = entry.getValue();
			if (!busy) {
				//dispatch to this sfp
				dispatchToSfp(sfp, sfApplicationRequest);
				entry.setValue(BUSY);
				return;
			}
		}
	}

	private void handleSfpNotBusy(SfpName sfpName) {
		Validate.isTrue(sfpActorMap.containsKey(sfpName));
		if (requestQueue.isEmpty()) {
			setActorBusyness(sfpName, NOT_BUSY);
		} else {
			SfApplicationRequest request = requestQueue.poll();
			dispatchToSfp(sfpName, request);
			setActorBusyness(sfpName, BUSY);
		}
	}

	private void dispatchToSfp(SfpName sfpName, SfApplicationRequest sfApplicationRequest) {
		logger.info("Dispatching request {} to SFP {}", sfApplicationRequest, sfpName);
		ActorRef actorRef = sfpActorMap.get(sfpName);
		actorRef.tell(sfApplicationRequest, getSelf());
	}

	private void handleSfpHeartbeatFailed(HeartbeatFailedMsg heartbeatFailedMsg) {
		SfpName sfpName = heartbeatFailedMsg.getSfpName();
		logger.warning("Removing SFP {}", sfpName.getName());
		sfpActorMap.remove(sfpName);
		sfpBusyMap.remove(sfpName);
		if (sfpActorMap.isEmpty()) {
			handleNoMoreSfpActors();
		}
	}

	private void handleNoMoreSfpActors() {
		String message = "There are no '" + simulationFunctionName.getName()
				+ "' SFPs available to handle requests.";
		logger.warning(message);
		Exception exception = new IllegalStateException(message);
		while (!requestQueue.isEmpty()) {
			requestQueue.poll().getCompletableFuture().completeExceptionally(exception);
		}
	}

	private void setActorBusyness(SfpName sfpName, boolean busy) {
		logger.debug("Setting actor busyness for {} to {}", sfpName, busy);
		this.sfpBusyMap.put(sfpName, busy);
	}

	/**
	 * Validates that the simulation function name of the new SFP matches the
	 * simulation function for which this SFP pool manager was created.
	 */
	private void validateSimFunctionName(NewSfpMsg newSfpMsg) {
		Validate.isTrue(this.simulationFunctionName.equals(newSfpMsg.getSimulationFunctionName()));
	}

	/**
	 * Validates that the simulation function name of the request matches the
	 * simulation function for which this SFP pool manager was created.
	 */
	private void validateSimFunctionName(SfApplicationRequest sfApplicationRequest) {
		Validate.isTrue(this.simulationFunctionName.equals(sfApplicationRequest.getSimulationFunctionName()));
	}
}

package edu.osu.sfal.actors;

import akka.actor.ActorRef;
import akka.actor.Props;
import com.google.common.collect.Maps;
import edu.osu.sfal.messages.SfApplicationRequest;
import edu.osu.sfal.messages.SfpNotBusy;
import edu.osu.sfal.messages.sfp.HeartbeatFailed;
import edu.osu.sfal.messages.sfp.NewSfp;
import edu.osu.sfal.util.SfpName;
import edu.osu.sfal.util.SimulationFunctionName;
import org.apache.commons.lang3.Validate;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

public class SfpPoolManager extends LastMessageReceivedActor {

	static final boolean BUSY = true, NOT_BUSY = false;

	private static AtomicInteger sfpActorCounter = new AtomicInteger();

	private final SimulationFunctionName simulationFunctionName;
	private final PropsFactory<SfpActor> sfpActorPropsFactory;

	final Queue<SfApplicationRequest> requestQueue = new LinkedList<>();
	final Map<SfpName, ActorRef> sfpActorMap = Maps.newHashMap();
	final Map<SfpName, Boolean> sfpBusyMap = Maps.newHashMap();

	public SfpPoolManager(
			SimulationFunctionName simulationFunctionName,
			PropsFactory<SfpActor> sfpActorPropsFactory) {
		this.simulationFunctionName = simulationFunctionName;
		this.sfpActorPropsFactory = sfpActorPropsFactory;
	}

	@Override
	public void onReceiveImpl(Object message) throws Exception {
		if(message instanceof NewSfp) {
			handleNewSfpRegistration((NewSfp) message);
		} else if(message instanceof SfApplicationRequest) {
			handleSfApplicationRequest((SfApplicationRequest) message);
		} else if(message instanceof SfpNotBusy) {
			handleSfpNotBusy(((SfpNotBusy) message).getSfpName());
		} else if(message instanceof HeartbeatFailed) {
			handleSfpHeartbeatFailed((HeartbeatFailed) message);
		} else {
			unhandled(message);
		}
	}

	private void handleNewSfpRegistration(NewSfp newSfp) {
		Validate.isTrue(simulationFunctionName.equals(newSfp.getSimulationFunctionName()));
		SfpName sfpName = newSfp.getSfpName();
		ActorRef actorRef = createSfpActor(sfpName);
		sfpActorMap.put(sfpName, actorRef);
		sfpBusyMap.put(sfpName, NOT_BUSY);
		handleSfpNotBusy(sfpName);
	}

	private ActorRef createSfpActor(SfpName sfpName) {
		Props props = sfpActorPropsFactory.createProps(SfpActor.class, simulationFunctionName, sfpName);
		String name = sfpName.getName() + "_" + sfpActorCounter.incrementAndGet();
		return getContext().actorOf(props, name);
	}

	/**
	 * Handles the simulation function application request.
	 */
	private void handleSfApplicationRequest(SfApplicationRequest sfApplicationRequest) {
		validateSimFunctionName(sfApplicationRequest.getSimulationFunctionName());
		attemptToDispatchRequest(sfApplicationRequest);
	}

	/**
	 * Dispatches request to an SfpActor if any are not busy. If all are busy, enqueues request.
	 */
	private void attemptToDispatchRequest(SfApplicationRequest sfApplicationRequest) {
		if(isPoolEmptyOfSFPs()) {
			handlePoolEmptyOfSFPs(sfApplicationRequest);
		} else if(allPooledSFPsAreBusy()) {
			requestQueue.add(sfApplicationRequest);
		} else {
			dispatchRequest(sfApplicationRequest);
		}
	}

	private boolean isPoolEmptyOfSFPs() {
		return sfpActorMap.isEmpty();
	}

	private void handlePoolEmptyOfSFPs(SfApplicationRequest sfApplicationRequest) {
		throw new IllegalStateException("Received SfApplicationRequest when pool of SFPs is empty.");
	}

	private boolean allPooledSFPsAreBusy() {
		Validate.isTrue(!sfpBusyMap.isEmpty());
		for(boolean busy : sfpBusyMap.values()) {
			if(!busy) {
				return false;
			}
		}
		return true;
	}

	private void dispatchRequest(SfApplicationRequest sfApplicationRequest) {
		// may want to implement preferred order in the future.. or now we just grab the first one
		for(Map.Entry<SfpName,Boolean> entry : sfpBusyMap.entrySet()) {
			SfpName sfp = entry.getKey();
			boolean busy = entry.getValue();
			if(!busy) {
				//dispatch to this sfp
				dispatchToSfp(sfp, sfApplicationRequest);
				entry.setValue(BUSY);
				return;
			}
		}
	}

	private void handleSfpNotBusy(SfpName sfpName) {
		Validate.isTrue(sfpActorMap.containsKey(sfpName));
		if(requestQueue.isEmpty()) {
			setActorBusyness(sfpName, NOT_BUSY);
		} else {
			SfApplicationRequest request = requestQueue.poll();
			dispatchToSfp(sfpName, request);
			setActorBusyness(sfpName, BUSY);
		}
	}

	private void dispatchToSfp(SfpName sfpName, SfApplicationRequest sfApplicationRequest) {
		ActorRef actorRef = sfpActorMap.get(sfpName);
		actorRef.tell(sfApplicationRequest, getSelf());
	}

	private void handleSfpHeartbeatFailed(HeartbeatFailed heartbeatFailedMsg) {
		SfpName sfpName = heartbeatFailedMsg.getSfpName();
		sfpActorMap.remove(sfpName);
		sfpBusyMap.remove(sfpName);
		if(sfpActorMap.isEmpty()) {
			handleNoMoreSfpActors();
		}
	}

	private void handleNoMoreSfpActors() {
		String exceptionMessage = "There are no " + simulationFunctionName.getName()
				+ " SFPs available to handle requests.";
		Exception exception = new IllegalStateException(exceptionMessage);
		while(!requestQueue.isEmpty()) {
			requestQueue.poll().getCompletableFuture().completeExceptionally(exception);
		}
		getContext().stop(getSelf());
	}

	private void setActorBusyness(SfpName sfpName, boolean busy) {
		this.sfpBusyMap.put(sfpName, busy);
	}

	private void validateSimFunctionName(SimulationFunctionName simFuncName) {
		Validate.isTrue(this.simulationFunctionName.equals(simFuncName));
	}
}

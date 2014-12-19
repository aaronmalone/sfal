package edu.osu.sfal.actors;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import edu.osu.lapis.LapisApi;
import edu.osu.sfal.exception.NoSfpAvailableException;
import edu.osu.sfal.messages.SfApplicationRequest;
import edu.osu.sfal.messages.sfp.NewSfpMsg;
import edu.osu.sfal.util.SimulationFunctionName;
import org.apache.commons.lang3.Validate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Actor to manage all SFPs. There should only be one per SFAL instance.
 */
public class SfpGeneralManager extends UntypedActor {

	private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

	/**
	 * Map of simulation function name to actor ref. for SfpPoolManager
	 */
	final Map<SimulationFunctionName, ActorRef> sfpPoolMap = new HashMap<>();

	/**
	 * The LAPIS client.
	 */
	private final LapisApi lapisApi;

	public SfpGeneralManager(LapisApi lapisApi) {
		Validate.notNull(lapisApi);
		this.lapisApi = lapisApi;
	}

	@Override
	public void onReceive(Object message) throws Exception {
		logger.info("Received message: {}", message);
		logger.debug("Current thread: {}", Thread.currentThread());
		if (message instanceof NewSfpMsg) {
			handleNewSfp((NewSfpMsg) message);
		} else if (message instanceof SfApplicationRequest) {
			handleSfApplicationRequest((SfApplicationRequest) message);
		}
	}

	private void handleNewSfp(NewSfpMsg newSfp) {
		SimulationFunctionName sf = newSfp.getSimulationFunctionName();
		logger.info("New SFP {} for simulation function {}", newSfp.getSfpName(), sf.getName());
		final ActorRef actorRef;
		if (sfpPoolMap.containsKey(sf)) {
			actorRef = sfpPoolMap.get(sf);
		} else {
			actorRef = createAndStoreSfpPoolManagerActor(sf);
		}
		actorRef.tell(newSfp, getSelf());
	}

	private void handleSfApplicationRequest(SfApplicationRequest sfApplicationRequest) {
		SimulationFunctionName sf = sfApplicationRequest.getSimulationFunctionName();
		if (sfpPoolMap.containsKey(sf)) {
			ActorRef actorRef = sfpPoolMap.get(sf);
			logger.info("Passing request to SFP: {}", actorRef);
			actorRef.tell(sfApplicationRequest, getSelf());
		} else {
			CompletableFuture completableFuture = sfApplicationRequest.getCompletableFuture();
			completableFuture.completeExceptionally(
					new NoSfpAvailableException(sf, "Received a request, but no SFP pool can handle it."));
		}
	}

	private ActorRef createAndStoreSfpPoolManagerActor(SimulationFunctionName sfName) {
		logger.info("Creating new {} for simulation function {}", SfpPoolManager.class.getName(), sfName.getName());
		Props props = Props.create(SfpPoolManager.class, sfName, lapisApi);
		ActorRef ref = getContext().actorOf(props, SfpPoolManager.class.getSimpleName() + "_" + sfName.getName());
		logger.debug("Created actor {}", ref);
		sfpPoolMap.put(sfName, ref);
		return ref;
	}
}

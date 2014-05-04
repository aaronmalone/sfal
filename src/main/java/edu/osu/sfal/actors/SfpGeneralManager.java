package edu.osu.sfal.actors;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import edu.osu.lapis.LapisApi;
import edu.osu.sfal.messages.SfApplicationRequest;
import edu.osu.sfal.messages.sfp.NewSfp;
import edu.osu.sfal.util.SimulationFunctionName;
import org.apache.commons.lang3.Validate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class SfpGeneralManager extends UntypedActor {

	private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

	final Map<SimulationFunctionName, ActorRef> sfpPoolMap = new HashMap<>();

	private final LapisApi lapisApi;

	public SfpGeneralManager(LapisApi lapisApi) {
		Validate.notNull(lapisApi);
		this.lapisApi = lapisApi;
	}

	@Override
	public void onReceive(Object message) throws Exception {
		logger.info("Received message: {}", message);
		if(message instanceof NewSfp) {
			handleNewSfp((NewSfp) message);
		} else if(message instanceof SfApplicationRequest) {
			handleSfApplicationRequest((SfApplicationRequest) message);
		}
	}

	private void handleNewSfp(NewSfp newSfp) {
		SimulationFunctionName sf = newSfp.getSimulationFunctionName();
		final ActorRef actorRef;
		if(sfpPoolMap.containsKey(sf)) {
			actorRef = sfpPoolMap.get(sf);
		} else {
			actorRef = createAndStoreSfpPoolManagerActor(sf);
		}
		actorRef.tell(newSfp, getSelf());
	}

	private void handleSfApplicationRequest(SfApplicationRequest sfApplicationRequest) {
		SimulationFunctionName sf = sfApplicationRequest.getSimulationFunctionName();
		if(sfpPoolMap.containsKey(sf)) {
			ActorRef actorRef = sfpPoolMap.get(sf);
			logger.info("Passing request to SFP: {}", actorRef);
			actorRef.tell(sfApplicationRequest, getSelf());
		} else {
			CompletableFuture completableFuture = sfApplicationRequest.getCompletableFuture();
			completableFuture.completeExceptionally(
					new IllegalStateException("Received a request, but no SFP pool can handle it."));
		}
	}

	private ActorRef createAndStoreSfpPoolManagerActor(SimulationFunctionName sfName) {
		Props props = Props.create(SfpPoolManager.class, sfName, lapisApi);
		ActorRef ref = getContext().actorOf(props, sfName.getName());
		sfpPoolMap.put(sfName, ref);
		return ref;
	}
}

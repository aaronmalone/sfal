package edu.osu.sfal.actors;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import edu.osu.sfal.messages.NewSfp;
import edu.osu.sfal.messages.SfApplicationRequest;
import edu.osu.sfal.util.SimulationFunctionName;

import java.util.HashMap;
import java.util.Map;

public class SfpGeneralManager extends LastMessageReceivedActor {

	final Map<SimulationFunctionName, ActorRef> sfpPoolMap = new HashMap<>();

	@Override
	public void onReceiveImpl(Object message) throws Exception {
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
			actorRef.tell(sfApplicationRequest, getSelf());
		} else {
			throw new IllegalStateException("Received a request, but no SFP pool can handle it.");
		}
	}

	private ActorRef createAndStoreSfpPoolManagerActor(SimulationFunctionName sfName) {
		ActorRef ref = getContext().actorOf(Props.create(SfpPoolManager.class), sfName.getName());
		ref.tell(sfName, getSender()); //initialize with SF name
		sfpPoolMap.put(sfName, ref);
		return ref;
	}
}

package edu.osu.sfal.actors;

import akka.actor.ActorRef;
import org.apache.commons.lang3.Validate;

public class ActorDelegate {

	private final ActorRef selfActorRef;

	public ActorDelegate(ActorRef selfActorRef) {
		Validate.notNull(selfActorRef);
		this.selfActorRef = selfActorRef;
	}

	/**
	 * Sends the message to the receiver.
	 */
	public void tell(ActorRef receiver, Object msg) {
		receiver.tell(msg, selfActorRef);
	}
}

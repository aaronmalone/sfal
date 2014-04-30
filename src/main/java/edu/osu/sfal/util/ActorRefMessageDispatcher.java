package edu.osu.sfal.util;

import akka.actor.ActorRef;
import org.apache.commons.lang3.Validate;

public class ActorRefMessageDispatcher<T> implements MessageDispatcher<T> {

	private final ActorRef destinationActorRef;

	public ActorRefMessageDispatcher(ActorRef destinationActorRef) {
		Validate.notNull(destinationActorRef, "actorRef cannot be null");
		this.destinationActorRef = destinationActorRef;
	}

	@Override
	public void dispatch(T message) {
		destinationActorRef.tell(message, ActorRef.noSender());
	}
}

package edu.osu.sfal.actors;

import akka.actor.UntypedActor;
import edu.osu.sfal.messages.GetLastMessage;

public abstract class LastMessageReceivedActor extends UntypedActor {

	private Object lastMessageReceived;

	public abstract void onReceiveImpl(Object message) throws Exception;

	@Override
	public final void onReceive(Object message) throws Exception {
		if(message instanceof GetLastMessage) {
			getSender().tell(lastMessageReceived, getSelf());
		} else {
			lastMessageReceived = message;
			onReceiveImpl(message);
		}
	}
}

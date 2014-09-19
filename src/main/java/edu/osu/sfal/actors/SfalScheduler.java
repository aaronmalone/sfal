package edu.osu.sfal.actors;

import akka.actor.ActorRef;
import akka.actor.Scheduler;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.FiniteDuration;

public class SfalScheduler {

	private final ActorRef senderActorRef;
	private final ExecutionContext executionContext;
	private final Scheduler scheduler;

	public SfalScheduler(ActorRef senderActorRef, ExecutionContext executionContext, Scheduler scheduler) {
		this.senderActorRef = senderActorRef;
		this.executionContext = executionContext;
		this.scheduler = scheduler;
	}

	/**
	 * Schedules the sending of a message to the receiver after the specified
	 * delay.
	 */
	public void scheduleOnce(FiniteDuration delay, ActorRef receiver, Object msg) {
		scheduler.scheduleOnce(delay, receiver, msg, executionContext, senderActorRef);
	}
}

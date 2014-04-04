package edu.osu.sfal.actors.creators;

import akka.actor.Props;

public interface PropsFactory<T> {
	default Props createProps(Class<T> cls, Object... args) {
		return Props.create(cls, args);
	}
}

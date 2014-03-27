package edu.osu.sfal.util;

public interface MessageDispatcher<T> {
	public void dispatch(T message);
}

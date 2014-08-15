package edu.osu.sfal.rest;

import org.restlet.Message;

public class AttributeUtil {

	public static <T> T getAttribute(Message message, String name, Class<T> cls) {
		Object value = message.getAttributes().get(name);
		return value != null ? cls.cast(value) : null;
	}

	public static void setAttribute(Message message, String name, Object value) {
		message.getAttributes().put(name, value);
	}
}

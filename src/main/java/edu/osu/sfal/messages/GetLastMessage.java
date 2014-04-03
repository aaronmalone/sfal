package edu.osu.sfal.messages;

import com.google.common.annotations.VisibleForTesting;

@VisibleForTesting
public class GetLastMessage {

	public static final GetLastMessage INSTANCE = new GetLastMessage();

	private GetLastMessage() {}

}

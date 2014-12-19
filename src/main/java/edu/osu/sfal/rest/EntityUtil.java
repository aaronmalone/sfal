package edu.osu.sfal.rest;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.restlet.Message;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.ResourceException;

import java.io.IOException;
import java.io.Reader;

public class EntityUtil {

	private static final JsonParser jsonParser = new JsonParser();

	public static JsonElement getEntityJson(Message message) {
		return getEntityJson(message.getEntity());
	}

	public static JsonElement getEntityJson(Representation entity) {
		try (Reader reader = entity.getReader()) {
			return jsonParser.parse(reader);
		} catch (IOException e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
		} catch (JsonSyntaxException e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, e);
		}
	}
}

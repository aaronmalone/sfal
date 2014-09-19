package edu.osu.sfal.rest;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.ResourceException;

import java.io.IOException;
import java.io.Reader;

public class EntityUtil {

	private static final JsonParser jsonParser = new JsonParser();

	public static JsonElement getEntityJson(Representation entity) {
		try (Reader reader = entity.getReader()) {
			return jsonParser.parse(reader);
		} catch (IOException e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
		}
	}
}

package edu.osu.sfal.rest;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.routing.Filter;

import java.io.IOException;
import java.io.Reader;

import static edu.osu.sfal.rest.AttributeUtil.setAttribute;

public class JsonEntityExtractor extends Filter {

	public static final String ENTITY_ATTRIBUTE_NAME = "ENTITY_ATTR";

	private final JsonParser jsonParser = new JsonParser();

	public JsonEntityExtractor(Restlet next) {
		setNext(next);
	}

	@Override
	protected int beforeHandle(Request request, Response response) {
		if (request.getEntity().getSize() != 0) {
			extractAndSetJsonEntityAttribute(request);
		}
		return CONTINUE;
	}

	private void extractAndSetJsonEntityAttribute(Request request) {
		final JsonElement jsonElement;
		try (Reader reader = request.getEntity().getReader()) {
			jsonElement = jsonParser.parse(reader);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		setAttribute(request, "ENTITY_ATTR", jsonElement);
	}
}

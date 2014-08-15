package edu.osu.sfal.rest;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import org.junit.Assert;
import org.junit.Test;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.Method;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.StringRepresentation;

import static edu.osu.sfal.rest.AttributeUtil.getAttribute;
import static edu.osu.sfal.rest.JsonEntityExtractor.ENTITY_ATTRIBUTE_NAME;

public class JsonEntityExtractorTest {

	private final Restlet noOp = new Restlet() {
		@Override
		public void handle(Request request, Response response) {
			super.handle(request, response);
			//do nothing else
		}
	};

	private final JsonEntityExtractor jsonEntityExtractor = new JsonEntityExtractor(noOp);
	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

	@Test
	public void testJsonEntityExtractedToAttribute() {
		String json = gson.toJson(ImmutableMap.of("abd", 123, "qwerty", "fit-up"));
		Request request = new Request(Method.POST, "resource", new StringRepresentation(json));
		Assert.assertNull(getAttribute(request, ENTITY_ATTRIBUTE_NAME, Object.class));
		jsonEntityExtractor.handle(request);
		JsonElement jsonElement = getAttribute(request, ENTITY_ATTRIBUTE_NAME, JsonElement.class);
		Assert.assertNotNull("Attribute should be set", jsonElement);
	}

	@Test
	public void emptyEntityNotExtractedToAttribute() {
		Request request = new Request(Method.POST, "resource", new EmptyRepresentation());
		jsonEntityExtractor.handle(request);
		Assert.assertNull("Attribute should not have been set",
				getAttribute(request, ENTITY_ATTRIBUTE_NAME, JsonElement.class));
	}
}

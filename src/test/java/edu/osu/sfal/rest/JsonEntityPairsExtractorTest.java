package edu.osu.sfal.rest;

import com.google.common.collect.ImmutableMap;
import com.google.gson.*;
import org.junit.Assert;
import org.junit.Test;
import org.restlet.Request;
import org.restlet.Restlet;
import org.restlet.data.Method;
import org.restlet.representation.StringRepresentation;

import java.util.Map;

public class JsonEntityPairsExtractorTest {

	private final String
			STRING = "string",
			NULL = "null",
			INTEGER = "integer",
			OBJECT = "object",
			DOUBLE = "double",
			ARRAY = "array";

	private final Object entity = ImmutableMap.builder()
			.put(STRING, "This is a string.")
			.put(NULL, JsonNull.INSTANCE)
			.put(INTEGER, 100)
			.put(OBJECT, ImmutableMap.of("key1", "value1", "two", 2))
			.put(DOUBLE, 7.7)
			.put(ARRAY, new Object[]{"one", 2, "three", null}).build();

	private final Gson gson = new GsonBuilder()
			.serializeNulls()
			.setPrettyPrinting()
			.create();

	private final JsonEntityPairsExtractor extractor = new JsonEntityPairsExtractor(null);

	public JsonEntityPairsExtractorTest() {
		extractor.setNext(new Restlet() {
			//no override
		});
	}

	@Test
	public void testJsonEntityPairsExtraction() {
		String json = gson.toJson(entity);
		Request request = new Request(Method.POST, "resource", new StringRepresentation(json));
		extractor.handle(request);
		validateAttributes(request.getAttributes());
	}

	private void validateAttributes(Map<String, Object> attrs) {
		validatePresenceAndType(attrs, STRING, String.class);
		validatePresenceAndType(attrs, INTEGER, Number.class);
		validatePresenceAndType(attrs, OBJECT, JsonObject.class);
		validatePresenceAndType(attrs, DOUBLE, Number.class);
		validatePresenceAndType(attrs, ARRAY, JsonArray.class);
		Assert.assertNull("null attribute should not be added", attrs.get(NULL));
	}

	private void validatePresenceAndType(Map<String, Object> attrs, String key, Class<?> cls) {
		Object value = attrs.get(key);
		Assert.assertNotNull(value);
		Assert.assertTrue("Attribute object '" + value + "' is not of expected type: " + cls.getName(),
				cls.isInstance(value));
	}
}

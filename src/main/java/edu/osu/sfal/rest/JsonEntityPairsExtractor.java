package edu.osu.sfal.rest;

import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static edu.osu.sfal.rest.AttributeUtil.getAttribute;

public class JsonEntityPairsExtractor extends JsonEntityExtractor {

	public JsonEntityPairsExtractor(Restlet next) {
        super(next);
    }

    @Override
    protected int beforeHandle(Request request, Response response) {
        int continueStatus = super.beforeHandle(request, response); //required for entity extraction
        if(continueStatus == CONTINUE) {
            extractPairsFromJsonEntityAndSetAttributes(request);
        }
        return continueStatus;
    }

    private void extractPairsFromJsonEntityAndSetAttributes(Request request) {
		JsonElement entity = getAttribute(request, ENTITY_ATTRIBUTE_NAME, JsonElement.class);
		Map<String, Object> pairsMap = extractPairsFromJsonEntity(entity);
		request.getAttributes().putAll(pairsMap);
	}

	//TODO ADD JAVADOC
	private Map<String, Object> extractPairsFromJsonEntity(JsonElement entity) {
		if (entity != null && entity.isJsonObject()) {
			Set<Map.Entry<String, JsonElement>> entrySet = entity.getAsJsonObject().entrySet();
			Map<String, Object> returnMap = Maps.newHashMapWithExpectedSize(entrySet.size());
			entrySet.forEach(entry -> {
				Object value = transformValue(entry.getValue());
				if(value != null) {
					returnMap.put(entry.getKey(), value);
				}
			});
			return returnMap;
		} else {
			return Collections.emptyMap();
		}
	}

	/**
	 * Transforms some JsonElement values to other types of Java objects.
	 * Strings, booleans, and numbers are transformed to java.lang.String values, primitive booleans, and
	 * java.lang.Number instances, respectively. JSON null values are transformed to null. JSON objects and JSON arrays
	 * are not transformed.
	 * <br><br>
	 * Note that this method does not change the state of any object passed as an argument. It returns corresponding
	 * objects for certain types of arguments.
	 */
	private Object transformValue(JsonElement jsonElement) {
		if(jsonElement.isJsonPrimitive()) {
			return transformJsonPrimitive(jsonElement.getAsJsonPrimitive());
		} else if(jsonElement.isJsonNull()) {
			return null;
		} else {
			return jsonElement;
		}
	}

	private Object transformJsonPrimitive(JsonPrimitive primitive) {
		if(primitive.isString()) {
			return primitive.getAsString();
		} else if(primitive.isBoolean()) {
			return primitive.getAsBoolean();
		} else if(primitive.isNumber()) {
			return primitive.getAsNumber();
		} else {
			throw new IllegalArgumentException("Unable to transform JSON primitive: " + primitive);
		}
	}
}

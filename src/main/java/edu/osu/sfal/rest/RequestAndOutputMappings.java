package edu.osu.sfal.rest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import edu.osu.sfal.messages.SfApplicationRequest;
import edu.osu.sfal.util.SimulationFunctionName;
import org.apache.commons.lang3.Validate;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

public class RequestAndOutputMappings {

	private final SfApplicationRequest sfApplicationRequest;

	/** Map of output name (LAPIS variable name) to data store key */
	private final Map<String, String> outputMappings;

	public RequestAndOutputMappings(SfApplicationRequest sfApplicationRequest, Map<String, String> outputMappings) {
		this.sfApplicationRequest = sfApplicationRequest;
		this.outputMappings = outputMappings;
	}

	public SfApplicationRequest getSfApplicationRequest() {
		return sfApplicationRequest;
	}

	public Map<String, String> getOutputMappings() {
		return outputMappings;
	}

	public static RequestAndOutputMappings getFromJson(JsonObject json, Function<String, Object> dataAccess) {
		SimulationFunctionName simulationFunctionName = getSimulationFunctionName(json);
		int timestep = getTimestep(json);
		Map<String, Object> inputs = getInputs(json, dataAccess);
		Map<String, String> outputMappings = getOutputMappings(json);
		SfApplicationRequest sfApplicationRequest = new SfApplicationRequest(simulationFunctionName,
				timestep, inputs, outputMappings.keySet());
		return new RequestAndOutputMappings(sfApplicationRequest, outputMappings);
	}

	private static JsonElement getProperty(JsonObject jsonObject, String property) {
		JsonElement element = jsonObject.get(property);
		Validate.notNull(element, "Property '" + property + "' not present.");
		return element;
	}

	private static SimulationFunctionName getSimulationFunctionName(JsonObject json) {
		String modelName = getProperty(json, "model").getAsString();
		return new SimulationFunctionName(modelName);
	}

	private static int getTimestep(JsonObject json) {
		return getProperty(json, "timestep").getAsNumber().intValue();
	}

	private static Map<String, Object> getInputs(JsonObject json, Function<String, Object> dataAccess) {
		JsonObject inputsJson = getProperty(json, "inputs").getAsJsonObject();
		return getRequestInputs(inputsJson, dataAccess);
	}

	/**
	 * Takes as an argument the JSON object which maps input names to the
	 * datastore keys for the actual data objects that should be used as inputs
	 * in the requested calculation,  as well as a means of accessing data from
	 * the datastore.
	 * <br>
	 * Returns a map of the input names to the input data objects.
	 */
	private static Map<String, Object> getRequestInputs(JsonObject jsonObject, Function<String, Object> dataAccess) {
		Map<String, Object> returnMap = new HashMap<>();
		for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
			String inputName = entry.getKey();
			String dataStoreKey = entry.getValue().getAsString();
			Object value = dataAccess.apply(dataStoreKey);
			//no values should be null
			if (value == null) {
				throw new NullPointerException("null data for key " + dataStoreKey);
			}
			returnMap.put(inputName, value);
		}
		return returnMap;
	}

	/**
	 * Returns map of output name (LAPIS variable name) to data store key.
	 */
	private static Map<String, String> getOutputMappings(JsonObject json) {
		JsonObject outputsMappingsJson = getProperty(json, "outputs").getAsJsonObject();
		return toStringMap(outputsMappingsJson);
	}

	private static Map<String, String> toStringMap(JsonObject jsonObject) {
		return jsonObject
				.entrySet()
				.stream()
				.collect(toMap(entry -> entry.getKey(), entry -> entry.getValue().getAsString()));
	}
}

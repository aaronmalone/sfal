package edu.osu.sfal.rest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import edu.osu.sfal.data.InputValuesMap;
import edu.osu.sfal.messages.SfApplicationRequest;
import edu.osu.sfal.util.SimulationFunctionName;
import net.spy.memcached.compat.log.Logger;
import net.spy.memcached.compat.log.LoggerFactory;
import org.apache.commons.lang3.Validate;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.Method;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

import java.util.Map;

import static edu.osu.sfal.rest.EntityUtil.getEntityJson;
import static java.util.stream.Collectors.toMap;

public abstract class RequestRestletBase extends Restlet {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public final void handle(Request request, Response response) {
		Method method = request.getMethod();
		if (method.equals(Method.POST)) {
			if (!response.getStatus().isError()) {
				handlePost(request, response);
			} else {
				logger.warn("Response already has error status: " + response.getStatus());
			}
		} else {
			throw new ResourceException(Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
		}
	}

	private void handlePost(Request request, Response response) {
		JsonObject json = getEntityJson(request).getAsJsonObject();
		RequestAndOutputMappings requestAndOutputMappings = getFromJson(json);
		handleRequestAndOutputMappings(requestAndOutputMappings, response);
	}

	protected abstract void handleRequestAndOutputMappings(
			RequestAndOutputMappings requestAndOutputMappings,
			Response response);

	private RequestAndOutputMappings getFromJson(JsonObject json) {
		SimulationFunctionName simulationFunctionName = getSimulationFunctionName(json);
		int timestep = getTimestep(json);
		InputValuesMap inputs = getInputs(json);
		Map<String, String> outputMappings = getOutputMappings(json);
		SfApplicationRequest sfApplicationRequest = new SfApplicationRequest(simulationFunctionName,
				timestep, inputs, outputMappings.keySet());
		return new RequestAndOutputMappings(sfApplicationRequest, outputMappings);
	}

	private SimulationFunctionName getSimulationFunctionName(JsonObject json) {
		String modelName = getProperty(json, "model").getAsString();
		return new SimulationFunctionName(modelName);
	}

	private int getTimestep(JsonObject json) {
		return getProperty(json, "timestep").getAsNumber().intValue();
	}

	private InputValuesMap getInputs(JsonObject json) {
		JsonObject inputsJson = getProperty(json, "inputs").getAsJsonObject();
		return getRequestInputs(inputsJson);
	}

	/**
	 * Takes as an argument the JSON object which maps input names to the
	 * datastore keys for the actual data objects that should be used as inputs
	 * in the requested calculation,  as well as a means of accessing data from
	 * the datastore.
	 * <br>
	 * Returns a map of the input names to the input data objects.
	 */
	private InputValuesMap getRequestInputs(JsonObject jsonObject) {
		InputValuesMap returnMap = new InputValuesMap();
		for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
			String inputName = entry.getKey();
			String dataStoreKey = entry.getValue().getAsString();
			Object value = lookUpStoredValue(dataStoreKey);
			//no values should be null
			if (value == null) {
				throw new NullPointerException("null data for key " + dataStoreKey);
			}
			returnMap.put(inputName, value);
		}
		return returnMap;
	}

	protected abstract Object lookUpStoredValue(String dataStoreKey);

	/**
	 * Returns map of output name (LAPIS variable name) to data store key.
	 */
	private Map<String, String> getOutputMappings(JsonObject json) {
		JsonObject outputsMappingsJson = getProperty(json, "outputs").getAsJsonObject();
		return toStringMap(outputsMappingsJson);
	}

	private Map<String, String> toStringMap(JsonObject jsonObject) {
		return jsonObject
				.entrySet()
				.stream()
				.collect(toMap(entry -> entry.getKey(), entry -> entry.getValue().getAsString()));
	}

	private JsonElement getProperty(JsonObject jsonObject, String property) {
		JsonElement element = jsonObject.get(property);
		Validate.notNull(element, "Property '" + property + "' not present.");
		return element;
	}


}

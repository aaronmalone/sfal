package edu.osu.sfal.rest;

import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import edu.osu.sfal.data.SfalDao;
import edu.osu.sfal.messages.SfApplicationRequest;
import edu.osu.sfal.messages.SfApplicationResult;
import edu.osu.sfal.util.MessageDispatcher;
import edu.osu.sfal.util.SimulationFunctionName;
import org.apache.commons.lang3.Validate;
import org.apache.log4j.Logger;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static edu.osu.sfal.rest.AttributeUtil.getAttribute;
import static edu.osu.sfal.rest.JsonEntityExtractor.ENTITY_ATTRIBUTE_NAME;

public class IncomingRequestRestlet extends Restlet {

	private final Logger logger = Logger.getLogger(getClass());

	private final SfalDao sfalDao;
	private final MessageDispatcher<SfApplicationRequest> requestDispatcher;
	private final long timeoutMillis;

	public IncomingRequestRestlet(SfalDao sfalDao,
			MessageDispatcher<SfApplicationRequest> requestDispatcher,
			long timeoutMillis) {
		this(null, sfalDao, requestDispatcher, timeoutMillis);
	}

	public IncomingRequestRestlet(Context context, SfalDao sfalDao,
			MessageDispatcher<SfApplicationRequest> requestDispatcher,
			long timeoutMillis) {
		super(context);
		this.sfalDao = sfalDao;
		this.requestDispatcher = requestDispatcher;
		this.timeoutMillis = timeoutMillis;
	}

	@Override
	public void handle(Request request, Response response) {
		super.handle(request, response);
		logger.trace("Received incoming request: " + request);
		JsonObject jsonObject = getEntityAsJsonObject(request);
		SfApplicationRequest sfApplicationRequest = toSfApplicationRequest(jsonObject);
		requestDispatcher.dispatch(sfApplicationRequest);
		Map<String, String> outputsToDataStoreKeys = toStringMap(jsonObject.get("outputs").getAsJsonObject());
		saveResults(outputsToDataStoreKeys, getResult(sfApplicationRequest));
	}

	private JsonObject getEntityAsJsonObject(Request request) {
		JsonElement jsonElement = getAttribute(request, ENTITY_ATTRIBUTE_NAME, JsonElement.class);
		Validate.notNull(jsonElement, "No JSON entity attribute value.");
		return jsonElement.getAsJsonObject();
	}

	private SfApplicationRequest toSfApplicationRequest(JsonObject jsonObject) {
		String model = jsonObject.get("model").getAsString();
		SimulationFunctionName simulationFunctionName = new SimulationFunctionName(model);
		int timestep = jsonObject.get("timestep").getAsInt();
		Map<String, Object> inputs = getSfApplicationRequestInputs(jsonObject);
		Map<String, String> outputsMap = toStringMap(jsonObject.get("outputs").getAsJsonObject());
		return new SfApplicationRequest(simulationFunctionName, timestep, inputs, outputsMap.keySet());
	}

	private Map<String, Object> getSfApplicationRequestInputs(JsonObject jsonObject) {
		Map<String, String> inputNameToDataStoreKey = toStringMap(jsonObject.get("inputs").getAsJsonObject());
		Map<String, Object> returnMap = new HashMap<>();
		inputNameToDataStoreKey.forEach( (inputName, dataStoreKey) -> {
					Object value = sfalDao.lookup(dataStoreKey);
					returnMap.put(inputName, value);
				}
		);
		return returnMap;
	}

	private Map<String,String> toStringMap(JsonObject jsonObject) {
		Map<String,String> map = Maps.newHashMap();
		jsonObject
				.entrySet()
				.forEach(entry -> {
					String value = entry.getValue().getAsString();
					map.put(entry.getKey(), value);
				});
		return map;
	}

	private SfApplicationResult getResult(SfApplicationRequest request) {
		try {
			Future<SfApplicationResult> future = request.getCompletableFuture();
			return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			throw new RuntimeException(e);
		}
	}

	private void saveResults(Map<String, String> outputNameToDataStoreKey, SfApplicationResult result) {
		Map<String, Object> outputValuesMap = result.getOutputs();
		outputNameToDataStoreKey.forEach(
				(outputName, dataStoreKey) -> {
					Object value = outputValuesMap.get(outputName);
					sfalDao.save(dataStoreKey, value);
				}

		);
	}
}

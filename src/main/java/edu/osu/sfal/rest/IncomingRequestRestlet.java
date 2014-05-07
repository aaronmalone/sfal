package edu.osu.sfal.rest;

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import edu.osu.sfal.data.SfalDao;
import edu.osu.sfal.messages.SfApplicationRequest;
import edu.osu.sfal.messages.SfApplicationResult;
import edu.osu.sfal.util.MessageDispatcher;
import edu.osu.sfal.util.SimulationFunctionName;
import org.apache.log4j.Logger;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.MediaType;
import org.restlet.data.Status;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static edu.osu.sfal.rest.AttributeUtil.getAttribute;

public class IncomingRequestRestlet extends Restlet {

	private final Logger logger = Logger.getLogger(getClass());

	private final SfalDao sfalDao;
	private final MessageDispatcher<SfApplicationRequest> requestDispatcher;
	private final long timeoutMillis;

	public IncomingRequestRestlet(SfalDao sfalDao,
			MessageDispatcher<SfApplicationRequest> requestDispatcher,
			long timeoutMillis) {
		this.sfalDao = sfalDao;
		this.requestDispatcher = requestDispatcher;
		this.timeoutMillis = timeoutMillis;
	}

	@Override
	public void handle(Request request, Response response) {
		super.handle(request, response);
		logger.debug("Received incoming request: " + request);
		if(!response.getStatus().isError()) {
			handleInternal(request, response);
		} else {
			logger.warn("Response has error status: " + response.getStatus());
		}
	}

	private void handleInternal(Request request, Response response) {
		try {
			SfApplicationRequest sfApplicationRequest = toSfApplicationRequest(request);
			Map<String, String> outputsToDataKeys = toStringMap(getAttribute(request, "outputs", JsonObject.class));
			requestDispatcher.dispatch(sfApplicationRequest);
			SfApplicationResult result = waitForResults(sfApplicationRequest);
			saveResults(outputsToDataKeys, result);
			response.setStatus(Status.SUCCESS_NO_CONTENT);
			logger.debug("Successfully completed request.");
		} catch(Exception e) {
			Throwable cause = Throwables.getRootCause(e);
			logger.warn("Exception while processing request.", e);
			response.setStatus(Status.SERVER_ERROR_INTERNAL, cause);
			response.setEntity("Exception while processing request: " + cause.getMessage(), MediaType.TEXT_PLAIN);
		}
	}

	private SfApplicationRequest toSfApplicationRequest(Request request) {
		String modelName = getAttribute(request, "model", String.class);
		SimulationFunctionName simulationFunctionName = new SimulationFunctionName(modelName);
		int timestep = getAttribute(request, "timestep", Number.class).intValue();
		Map<String, Object> inputs = getSfApplicationRequestInputs(getAttribute(request, "inputs", JsonObject.class));
		Map<String, String> outputsMap = toStringMap(getAttribute(request, "outputs", JsonObject.class));
		return new SfApplicationRequest(simulationFunctionName, timestep, inputs, outputsMap.keySet());
	}

	private Map<String, Object> getSfApplicationRequestInputs(JsonObject jsonObject) {
		Map<String, Object> returnMap = new HashMap<>();
		for(Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
			String inputName = entry.getKey();
			String dataStoreKey = entry.getValue().getAsString();
			Object value = sfalDao.lookup(dataStoreKey);
			//no values should be null
			if(value == null) {
				throw new NullPointerException("null data for key " + dataStoreKey);
			}
			returnMap.put(inputName, value);
		}
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

	private SfApplicationResult waitForResults(SfApplicationRequest request) throws Exception {
		logger.trace("Waiting for results...");
		Future<SfApplicationResult> future = request.getCompletableFuture();
		return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
	}

	private void saveResults(Map<String, String> outputNameToDataStoreKey, SfApplicationResult result) {
		logger.trace("Saving results...");
		Map<String, Object> outputValuesMap = result.getOutputs();
		outputNameToDataStoreKey.forEach(
				(outputName, dataStoreKey) -> {
					Object value = outputValuesMap.get(outputName);
					sfalDao.save(dataStoreKey, value);
				}
		);
	}
}

package edu.osu.sfal.rest;

import com.google.common.base.Throwables;
import com.google.gson.JsonObject;
import edu.osu.sfal.data.OutputSaverUtil;
import edu.osu.sfal.data.SfalDao;
import edu.osu.sfal.messages.SfApplicationRequest;
import edu.osu.sfal.messages.SfApplicationResult;
import edu.osu.sfal.util.MessageDispatcher;
import org.apache.log4j.Logger;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static edu.osu.sfal.rest.RequestAndOutputMappings.getFromJson;

/**
 * Restlet for serving requests to run calculations in the SFAL's network.
 */
public class IncomingRequestRestlet extends Restlet {

	private final Logger logger = Logger.getLogger(getClass());

	private final SfalDao sfalDao;
	private final MessageDispatcher<SfApplicationRequest> requestDispatcher;
	private final long timeoutMillis;

	public IncomingRequestRestlet(SfalDao sfalDao, MessageDispatcher<SfApplicationRequest> requestDispatcher, long timeoutMillis) {
		this.sfalDao = sfalDao;
		this.requestDispatcher = requestDispatcher;
		this.timeoutMillis = timeoutMillis;
	}

	@Override
	public void handle(Request request, Response response) {
		super.handle(request, response);
		logger.debug("Received incoming request: " + request);
		if (!request.getMethod().equals(Method.POST)) {
			throw new ResourceException(Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
		}
		if (!response.getStatus().isError()) {
			handleInternal(request, response);
		} else {
			logger.warn("Response already has error status: " + response.getStatus());
		}
	}

	private void handleInternal(Request request, Response response) {
		try {
			JsonObject jsonObject = EntityUtil.getEntityJson(request.getEntity()).getAsJsonObject();
			RequestAndOutputMappings requestAndOutputMappings = getFromJson(jsonObject, sfalDao::lookup);
			requestDispatcher.dispatch(requestAndOutputMappings.getSfApplicationRequest());
			SfApplicationResult result = waitForResults(requestAndOutputMappings.getSfApplicationRequest());
			saveResults(requestAndOutputMappings.getOutputMappings(), result);
			response.setStatus(Status.SUCCESS_NO_CONTENT);
			logger.debug("Successfully completed request.");
		} catch (Exception e) {
			Throwable cause = Throwables.getRootCause(e);
			logger.warn("Exception while processing request.", e);
			response.setStatus(Status.SERVER_ERROR_INTERNAL, cause);
			response.setEntity("Exception while processing request: " + cause.getMessage(), MediaType.TEXT_PLAIN);
		}
	}

	private SfApplicationResult waitForResults(SfApplicationRequest request) throws Exception {
		logger.trace("Waiting for results...");
		Future<SfApplicationResult> future = request.getCompletableFuture();
		return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
	}

	private void saveResults(Map<String, String> outputNameToDataStoreKey, SfApplicationResult result) {
		logger.trace("Saving results...");
		OutputSaverUtil.saveOutputs(sfalDao, outputNameToDataStoreKey, result);
	}
}

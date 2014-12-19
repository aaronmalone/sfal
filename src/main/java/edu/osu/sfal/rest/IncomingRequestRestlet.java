package edu.osu.sfal.rest;

import com.google.common.base.Throwables;
import edu.osu.sfal.data.OutputSaverUtil;
import edu.osu.sfal.data.SfalDao;
import edu.osu.sfal.exception.NoSfpAvailableException;
import edu.osu.sfal.messages.SfApplicationRequest;
import edu.osu.sfal.messages.SfApplicationResult;
import edu.osu.sfal.util.MessageDispatcher;
import org.restlet.Response;
import org.restlet.data.MediaType;
import org.restlet.data.Status;

import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Restlet for serving requests to run calculations in the SFAL's network.
 */
public class IncomingRequestRestlet extends RequestRestletBase {

	private final SfalDao sfalDao;
	private final MessageDispatcher<SfApplicationRequest> requestDispatcher;
	private final long timeoutMillis;

	public IncomingRequestRestlet(
			SfalDao sfalDao,
			MessageDispatcher<SfApplicationRequest> requestDispatcher,
			long timeoutMillis) {
		this.sfalDao = sfalDao;
		this.requestDispatcher = requestDispatcher;
		this.timeoutMillis = timeoutMillis;
	}

	@Override
	protected void handleRequestAndOutputMappings(RequestAndOutputMappings requestAndOutputMappings, Response response) {
		try {
			requestDispatcher.dispatch(requestAndOutputMappings.getSfApplicationRequest());
			SfApplicationResult result = waitForResults(requestAndOutputMappings.getSfApplicationRequest());
			saveResults(requestAndOutputMappings.getOutputMappings(), result);
			response.setStatus(Status.SUCCESS_NO_CONTENT);
			logger.debug("Successfully completed request.");
		} catch (Exception e) {
			Throwable cause = Throwables.getRootCause(e);
			logger.warn("Exception while processing request.", e);
			if (cause instanceof NoSfpAvailableException) {
				response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "No SFP available.");
				response.setEntity("No SFP available to process request.", MediaType.TEXT_PLAIN);
			} else {
				response.setStatus(Status.SERVER_ERROR_INTERNAL, cause);
				response.setEntity("Exception while processing request: " + cause.getMessage(), MediaType.TEXT_PLAIN);
			}
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

	@Override
	protected Object lookUpStoredValue(String dataStoreKey) {
		return sfalDao.lookup(dataStoreKey);
	}
}

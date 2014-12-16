package edu.osu.sfal.rest;

import edu.osu.sfal.data.OutputSaverUtil;
import edu.osu.sfal.data.SfalDao;
import edu.osu.sfal.messages.SfApplicationRequest;
import edu.osu.sfal.messages.SfApplicationResult;
import edu.osu.sfal.util.MessageDispatcher;
import org.restlet.Response;
import org.restlet.data.Status;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;

public class AsyncIncomingRequestRestlet extends RequestRestletBase {

	private final SfalDao sfalDao;
	private final MessageDispatcher<SfApplicationRequest> requestDispatcher;
	private final Executor asyncCompletionExecutor;

	public AsyncIncomingRequestRestlet(
			SfalDao sfalDao,
			MessageDispatcher<SfApplicationRequest> requestDispatcher,
			Executor asyncCompletionExecutor) {
		this.sfalDao = sfalDao;
		this.requestDispatcher = requestDispatcher;
		this.asyncCompletionExecutor = asyncCompletionExecutor;
	}

	@Override
	protected void handleRequestAndOutputMappings(
			RequestAndOutputMappings requestAndOutputMappings,
			Response response) {
		SfApplicationRequest sfApplicationRequest = requestAndOutputMappings.getSfApplicationRequest();
		requestDispatcher.dispatch(sfApplicationRequest);
		CompletableFuture<SfApplicationResult> completableFuture = sfApplicationRequest.getCompletableFuture();
		Map<String, String> outputMappings = requestAndOutputMappings.getOutputMappings();
		SfApplicationCompletionHandler callback = new SfApplicationCompletionHandler(outputMappings);
		completableFuture.handleAsync(callback, asyncCompletionExecutor);
		response.setStatus(Status.SUCCESS_CREATED);
	}

	@Override
	protected Object lookUpStoredValue(String dataStoreKey) {
		return sfalDao.lookup(dataStoreKey);
	}

	private class SfApplicationCompletionHandler implements BiFunction<SfApplicationResult, Throwable, Void> {

		final Map<String, String> outputMappings;

		SfApplicationCompletionHandler(Map<String, String> outputMappings) {
			this.outputMappings = outputMappings;
		}

		@Override
		public Void apply(SfApplicationResult sfApplicationResult, Throwable throwable) {
			if (throwable == null) {
				OutputSaverUtil.saveOutputs(sfalDao, outputMappings, sfApplicationResult);
				return null;
			} else {
				//TODO HANDLE
				return null;
			}
		}
	}
}

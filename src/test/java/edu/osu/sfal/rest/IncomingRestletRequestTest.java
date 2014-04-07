package edu.osu.sfal.rest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import edu.osu.sfal.data.SfalDao;
import edu.osu.sfal.data.SfalDaoInMemoryImpl;
import edu.osu.sfal.messages.SfApplicationRequest;
import edu.osu.sfal.messages.SfApplicationResult;
import edu.osu.sfal.util.MessageDispatcher;
import edu.osu.sfal.util.SfpName;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Method;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class IncomingRestletRequestTest {

	private final String OUTPUT_NAME = "output1",
		OUTPUT_VALUE_FOR = "outputValueFor-",
		STORED_INPUT_VALUE = "STORED INPUT VALUE",
		DATA_STORE_KEY_FOR_INPUT = "DATA_STORE_KEY_FOR_INPUT",
		DATA_STORE_KEY_FOR_OUTPUT = "DATA_STORE_KEY_FOR_OUTPUT",
		INPUT_NAME = "input1",
		SIMULATION_FUNCTION_NAME = "simFunctionName";

	@Test
	public void testHandle() {
		//the author apologizes for this ugly block of code
		final AtomicReference<SfApplicationRequest> requestReference = new AtomicReference<>();
		MessageDispatcher dispatcher = new MessageDispatcher<SfApplicationRequest>() {
			@Override public void dispatch(SfApplicationRequest sfAppReq) {
				requestReference.set(sfAppReq);
				SfApplicationResult result = getResultCorrespondingToRequest(sfAppReq);
				sfAppReq.getCompletableFuture().complete(result);
			}
		};
		SfalDao sfalDao = new SfalDaoInMemoryImpl();
		sfalDao.save(DATA_STORE_KEY_FOR_INPUT, STORED_INPUT_VALUE);
		IncomingRequestRestlet restlet = new IncomingRequestRestlet(sfalDao, dispatcher, 10);
		Response response = restlet.handle(getRequestWithJsonEntity());
		Assert.assertTrue(response.getStatus().isSuccess());


		//test all of the attributes of the SfApplicationRequest that was generated
		SfApplicationRequest sfAppRequest = requestReference.get();
		Assert.assertEquals(SIMULATION_FUNCTION_NAME, sfAppRequest.getSimulationFunctionName().getName());
		Assert.assertEquals(7, sfAppRequest.getTimestep());
		Assert.assertEquals(1, sfAppRequest.getInputs().size());
		Assert.assertEquals(STORED_INPUT_VALUE, sfAppRequest.getInputs().get(INPUT_NAME));
		Assert.assertEquals(1, sfAppRequest.getOutputNames().size());
		Assert.assertTrue(sfAppRequest.getOutputNames().contains(OUTPUT_NAME));

		//test that the outputs were saved
		Object savedOutput = sfalDao.lookup(DATA_STORE_KEY_FOR_OUTPUT);
		Assert.assertEquals(OUTPUT_VALUE_FOR + OUTPUT_NAME, savedOutput);
	}

	@Test
	public void testHandleWithException() {
		MessageDispatcher<SfApplicationRequest> exceptionDispatcher = new MessageDispatcher<SfApplicationRequest>() {
			@Override public void dispatch(SfApplicationRequest message) {
				throw new RuntimeException("This exception is what we expected for the test.");
			}
		};
		SfalDao sfalDao = new SfalDaoInMemoryImpl();
		IncomingRequestRestlet restlet = new IncomingRequestRestlet(null, sfalDao, exceptionDispatcher, 10);
		Response response = restlet.handle(getRequestWithJsonEntity());
		Assert.assertTrue(response.getStatus().isServerError());
	}

	private Request getRequestWithJsonEntity() {
		Request request = new Request(Method.POST, "resource");
		JsonElement jsonElement = getJsonElement();
		request.getAttributes().put(JsonEntityExtractor.ENTITY_ATTRIBUTE_NAME, jsonElement);
		return request;
	}

	private JsonElement getJsonElement() {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("model", SIMULATION_FUNCTION_NAME);
		jsonObject.addProperty("timestep", 7);
		jsonObject.add("inputs", getInputs());
		jsonObject.add("outputs", getOutputs());
		return jsonObject;
	}

	private JsonElement getInputs() {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty(INPUT_NAME, DATA_STORE_KEY_FOR_INPUT);
		return jsonObject;
	}

	private JsonElement getOutputs() {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty(OUTPUT_NAME, DATA_STORE_KEY_FOR_OUTPUT);
		return jsonObject;
	}

	private SfApplicationResult getResultCorrespondingToRequest(SfApplicationRequest request) {
		Map<String, Object> outputs = new HashMap<>();
		request.getOutputNames().forEach( name -> outputs.put(name, OUTPUT_VALUE_FOR+name));
		return new SfApplicationResult(request.getSimulationFunctionName(), request.getTimestep(),
				outputs, new SfpName(RandomStringUtils.randomAlphanumeric(8)));
	}
}

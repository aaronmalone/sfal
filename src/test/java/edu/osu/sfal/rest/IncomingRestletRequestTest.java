package edu.osu.sfal.rest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import edu.osu.sfal.data.OutputValuesMap;
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
import org.restlet.data.MediaType;
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

	private final int TIMESTEP = 7;

	@Test
	public void testHandle() {
		//the author apologizes for this ugly block of code
		final AtomicReference<SfApplicationRequest> requestReference = new AtomicReference<>();
		MessageDispatcher<SfApplicationRequest> dispatcher = request -> {
			requestReference.set(request);
			SfApplicationResult result = getResultCorrespondingToRequest(request);
			request.getCompletableFuture().complete(result);
		};
		SfalDao sfalDao = new SfalDaoInMemoryImpl();
		sfalDao.save(DATA_STORE_KEY_FOR_INPUT, STORED_INPUT_VALUE);
		IncomingRequestRestlet requestRestlet = new IncomingRequestRestlet(sfalDao, dispatcher, 10);
		Response response = requestRestlet.handle(getRequestWithJsonEntity());
		Assert.assertTrue(response.getStatus().isSuccess());

		//test all of the attributes of the SfApplicationRequest that was generated
		SfApplicationRequest request = requestReference.get();
		Assert.assertNotNull(request);
		Assert.assertEquals(SIMULATION_FUNCTION_NAME, request.getSimulationFunctionName().getName());
		Assert.assertEquals(TIMESTEP, request.getTimestep());
		Assert.assertEquals(1, request.getInputs().size());
		Assert.assertEquals(STORED_INPUT_VALUE, request.getInputs().get(INPUT_NAME));
		Assert.assertEquals(1, request.getOutputNames().size());
		Assert.assertTrue(request.getOutputNames().contains(OUTPUT_NAME));

		//test that the outputs were saved
		Object savedOutput = sfalDao.lookup(DATA_STORE_KEY_FOR_OUTPUT);
		Assert.assertEquals(OUTPUT_VALUE_FOR + OUTPUT_NAME, savedOutput);
	}

	@Test
	public void testHandleWithException() {
		MessageDispatcher<SfApplicationRequest> dispatcher = new MessageDispatcher<SfApplicationRequest>() {
			@Override
			public void dispatch(SfApplicationRequest message) {
				//do nothing
			}
		};
		SfalDao sfalDao = new SfalDaoInMemoryImpl();
		IncomingRequestRestlet restlet = new IncomingRequestRestlet(sfalDao, dispatcher, 10);
		try {
			Response response = restlet.handle(getRequestWithJsonEntity());
			Assert.fail();
		} catch (Exception e) {
			//expected
		}
	}

	private Request getRequestWithJsonEntity() {
		Request request = new Request(Method.POST, "resource");
		request.setEntity(getJsonElement().toString(), MediaType.APPLICATION_JSON);
		return request;
	}

	private JsonElement getJsonElement() {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("model", SIMULATION_FUNCTION_NAME);
		jsonObject.addProperty("timestep", TIMESTEP);
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
		Map<String, Object> outputsNameToValueMap = new HashMap<>();
		request.getOutputNames().forEach(name -> outputsNameToValueMap.put(name, OUTPUT_VALUE_FOR + name));
		return new SfApplicationResult(request.getSimulationFunctionName(), request.getTimestep(),
				OutputValuesMap.fromMap(outputsNameToValueMap), new SfpName(RandomStringUtils.randomAlphanumeric(8)));
	}
}

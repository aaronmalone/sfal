package edu.osu.sfal.util;

import com.couchbase.client.CouchbaseClient;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import edu.osu.lapis.util.Sleep;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ClientResource;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * A client to the SFAL that "drives" the simulation, in this case with the
 * weather model.
 * <p/>
 * This requires that an SFAL with connected weather model is already running.
 */
public class WeatherModelDriver {

	private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private static final String
			MODEL_NAME = "weather",
			COUCHBASE_URI = "http://bjyurkovich.com:8091/pools",
			REQUESTS_URI = "http://128.146.169.65:30000/requests",
			BASE_WEATHER_VARIABLE_NAME = "baseWeather", //output variable
			BASE_WEATHER_DATASTORE_KEY_PREFIX = "baseweather~";

	private static final ClientResource clientResource = new ClientResource(REQUESTS_URI);

	public static void main(String[] args) throws Exception {
		deletePreviousData();
		driveCalculations();
	}

	private static void deletePreviousData() throws ExecutionException, InterruptedException {
		CouchbaseClient client = getCouchbaseClient();
		System.out.println("Deleting previous data...");
		for (int i = 1; i < 101; ++i) {
			client.delete(BASE_WEATHER_DATASTORE_KEY_PREFIX + i).get();
		}
		client.shutdown();
		System.out.println("Previous data deleted.");
	}

	private static CouchbaseClient getCouchbaseClient() {
		try {
			List<URI> uriList = Lists.newArrayList(new URI(COUCHBASE_URI));
			return new CouchbaseClient(uriList, "default", "");
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
	}

	private static void driveCalculations() {
		Sleep.sleep(500);
		try {
			for (int i = 1; i < 21; i++) {
				System.out.print("Starting timestep " + i + "... ");
				driveCalculation(i);
				System.out.println("Finished timestep " + i);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	private static void driveCalculation(int timestep) {
		Map<String, Object> map = new HashMap<>();
		map.put("timestep", timestep);
		map.put("model", MODEL_NAME);
		map.put("inputs", new JsonObject()); //inputs will be empty
		map.put("outputs", getOutputs(timestep));
		StringRepresentation entity = new StringRepresentation(gson.toJson(map));
		System.out.println("about to post request...");
		clientResource.post(entity);
		System.out.println("request posted.");
	}

	private static Object getOutputs(int timestep) {
		Map<String, String> map = new HashMap<>();
		map.put(BASE_WEATHER_VARIABLE_NAME, BASE_WEATHER_DATASTORE_KEY_PREFIX + timestep);
		return map;
	}
}

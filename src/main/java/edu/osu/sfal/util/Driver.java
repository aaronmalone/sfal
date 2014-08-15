package edu.osu.sfal.util;

import com.couchbase.client.CouchbaseClient;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.osu.lapis.util.Sleep;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ClientResource;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A client to the SFAL that "drives" the simulation, in this case with the
 * weather model.
 *
 * This requires that an SFAL with connected weather model is already running.
 */
public class Driver {

	private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private static final String
			MODEL_NAME = "weather",
			COUCHBASE_URI = "http://bjyurkovich.com:8091/pools",
			REQUESTS_URI = "http://localhost:2345/requests",
			BASE_WEATHER_VARIABLE_NAME = "baseweather",
			PREVIOUS_WEATHER_VARIABLE_NAME = "previousWeather",
			BASE_WEATHER_DATASTORE_KEY_BASE = "baseweather~";
	private static final ClientResource clientResource = new ClientResource(REQUESTS_URI);

	public static void main(String[] args) throws Exception {
		deletePreviousData();
		driveCalculations();
	}

	private static void deletePreviousData() throws Exception {
		System.out.println("Deleting previous data...");
		List<URI> uriList = Lists.newArrayList(new URI(COUCHBASE_URI));
		CouchbaseClient client = new CouchbaseClient(uriList, "default", "");
		for (int i = 1; i < 101; ++i) {
			client.delete(BASE_WEATHER_DATASTORE_KEY_BASE + i).get();
		}
		client.shutdown();
		System.out.println("Previous data deleted.");
	}

	private static void driveCalculations() {
		Sleep.sleep(999);
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
		map.put("inputs", getInputs(timestep));
		map.put("outputs", getOutputs(timestep));
		StringRepresentation entity = new StringRepresentation(gson.toJson(map));
		clientResource.post(entity);
	}

	private static Object getInputs(int timestep) {
		return map(PREVIOUS_WEATHER_VARIABLE_NAME, BASE_WEATHER_DATASTORE_KEY_BASE + (timestep - 1));
	}

	private static Object getOutputs(int timestep) {
		return map(BASE_WEATHER_VARIABLE_NAME, BASE_WEATHER_DATASTORE_KEY_BASE + timestep);
	}

	private static Map<String, String> map(String key, String value) {
		Map<String, String> map = new HashMap<>();
		map.put(key, value);
		return map;
	}
}

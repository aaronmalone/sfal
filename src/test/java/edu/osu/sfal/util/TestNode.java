package edu.osu.sfal.util;

import com.couchbase.client.CouchbaseClient;
import com.google.common.collect.Lists;
import edu.osu.lapis.Flags;
import edu.osu.lapis.LapisApi;
import edu.osu.lapis.util.Sleep;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static edu.osu.sfal.actors.SfpActor.FINISHED_CALCULATING_VAR_NAME;
import static edu.osu.sfal.actors.SfpActor.READY_TO_CALCULATE_VAR_NAME;

public class TestNode {

	private static final String
			SIM_FUNCTION_NAME = "weather",
			COORDINATOR_ADDRESS = "http://localhost:8910",
			MY_NODE_ADDRESS = "http://localhost:8777";

	public static void main(String[] args) throws IOException, URISyntaxException {
		LapisApi lapisApi = new LapisApi("node-test1", COORDINATOR_ADDRESS, MY_NODE_ADDRESS);

		lapisApi.publishReadOnly("SIMULATION_FUNCTION_NAME", SIM_FUNCTION_NAME);

		double[] readyToCalculate = Flags.getFlag(false);
		lapisApi.publish(READY_TO_CALCULATE_VAR_NAME, readyToCalculate);

		double[] finishedCalculating = Flags.getFlag(true);
		lapisApi.publishReadOnly(FINISHED_CALCULATING_VAR_NAME, finishedCalculating);

		/* delete these */
		Object previousWeather = getPreviousWeather();
		lapisApi.publish("previousWeather", previousWeather);
		Object baseweather = previousWeather;
		lapisApi.publishReadOnly("baseweather", baseweather);

		int[] timestep = new int[1];
		lapisApi.publish("timestep", timestep);

		lapisApi.ready();

		while (true) {
			while (!Flags.evaluateFlagValue(readyToCalculate)) {
				Sleep.sleep(200);
			}
			Flags.setFlagFalse(finishedCalculating);
			Flags.setFlagFalse(readyToCalculate);

			System.out.println("would do calculation here...");
			System.out.println("timestep is " + timestep[0]);
//			baseweather[0] = new Random().nextDouble();
			baseweather = previousWeather;

			Sleep.sleep(1000);

			Flags.setFlagTrue(finishedCalculating);
		}

	}

	private static Object getPreviousWeather() throws IOException, URISyntaxException {
		List<URI> uriList = Lists.newArrayList(new URI("http://bjyurkovich.com:8091/pools"));
		CouchbaseClient client = new CouchbaseClient(uriList, "default", "");
		return client.get("baseweather~0");
	}
}

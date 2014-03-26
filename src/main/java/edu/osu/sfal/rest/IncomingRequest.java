package edu.osu.sfal.rest;

import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import edu.osu.sfal.util.SimulationFunctionName;

import java.util.Map;

public class IncomingRequest {

	public static IncomingRequest fromJson(JsonObject jsonObject) {
		String model = jsonObject.get("model").getAsString();
		int timestep = jsonObject.get("timestep").getAsInt();
		Map<String,String> inputs = toStringMap(jsonObject.get("inputs").getAsJsonObject());
		Map<String,String> outputs = toStringMap(jsonObject.get("outputs").getAsJsonObject());

		return new IncomingRequest(
				new SimulationFunctionName(model),
				timestep,
				inputs,
				outputs
		);
	}

	private static Map<String,String> toStringMap(JsonObject jsonObject) {
		Map<String,String> map = Maps.newHashMap();
		jsonObject
			.entrySet()
			.forEach( entry -> {
				String value = entry.getValue().getAsString();
				map.put(entry.getKey(), value);
			});
		return map;
	}

	private final SimulationFunctionName simulationFunctionName;
	private final int timestep;
	private final Map<String, String> inputs;
	private final Map<String, String> outputs;

	public IncomingRequest(SimulationFunctionName simulationFunctionName, int timestep,
	                       Map<String, String> inputs, Map<String, String> outputs) {
		this.simulationFunctionName = simulationFunctionName;
		this.timestep = timestep;
		this.inputs = inputs;
		this.outputs = outputs;
	}

	public SimulationFunctionName getSimulationFunctionName() {
		return simulationFunctionName;
	}

	public int getTimestep() {
		return timestep;
	}

	public Map<String, String> getInputs() {
		return inputs;
	}

	public Map<String, String> getOutputs() {
		return outputs;
	}
}

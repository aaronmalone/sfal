package edu.osu.sfal.data;

import edu.osu.sfal.messages.SfApplicationResult;

import java.util.Map;

public class OutputSaverUtil {

	public static void saveOutputs(SfalDao sfalDao, Map<String, String> outputMappings, SfApplicationResult result) {
		OutputValuesMap outputValuesMap = result.getOutputs();
		//outputMappings maps output (LAPIS variable) name to data store key
		outputMappings.forEach(
				(String outputVariableName, String dataStoreKey) -> {
					Object valueToSave = outputValuesMap.get(outputVariableName);
					sfalDao.save(dataStoreKey, valueToSave);
				}
		);
	}
}

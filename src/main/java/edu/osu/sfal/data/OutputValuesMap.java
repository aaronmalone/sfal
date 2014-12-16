package edu.osu.sfal.data;

import java.util.HashMap;
import java.util.Map;

/**
 * A map of output variable names (published LAPIS variables) to actual
 * output values.
 */
public class OutputValuesMap extends HashMap<String, Object> {

	public static OutputValuesMap fromMap(Map<String, Object> map) {
		OutputValuesMap o = new OutputValuesMap();
		o.putAll(map);
		return o;
	}
}

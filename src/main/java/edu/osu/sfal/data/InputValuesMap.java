package edu.osu.sfal.data;


import java.util.HashMap;
import java.util.Map;

/**
 * A map of input variable names (published LAPIS variables) to the values
 * which should be set for those variables.
 *
 * Note that this maps variable names to actual values, not data store keys.
 */
public class InputValuesMap extends HashMap<String, Object> {

	public static InputValuesMap fromMap(Map<String, Object> map) {
		InputValuesMap i = new InputValuesMap();
		i.putAll(map);
		return i;
	}
}

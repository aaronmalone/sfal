package edu.osu.sfal.util;

/**
 * This is the name of a simulation function process. It corresponds to a single
 * LAPIS node on the network which may have multiple nodes of the same type.
 */
public class SfpName extends StringKey {
	public SfpName(String name) {
        super(name);
    }
}

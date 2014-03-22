package edu.osu.sfal.util;

import org.apache.commons.lang3.Validate;

public class StringKey {
	
	private final String name;
	
	public StringKey(String name) {
		Validate.notNull(name, "name cannot be null");
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof StringKey) {
            return this.name.equals(((StringKey) obj).getName());
        } else {
            return false;
        }
	}

	@Override
	public String toString() {
		return name;
	}
}

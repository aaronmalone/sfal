package edu.osu.sfal.util;

import com.google.common.base.Preconditions;

public class StringKey implements Comparable<StringKey> {

	private final String name;
	private final String toStringValue;

	public StringKey(String name) {
		Preconditions.checkNotNull(name, "name cannot be null");
		this.name = name;
		toStringValue = getClass().getSimpleName() + "(" + name + ")";
	}

	public String getName() {
		return name;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object that) {
		if (getClass().isInstance(that)) {
			String thatName = ((StringKey) that).getName();
			return this.name.equals(thatName);
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		return this.toStringValue;
	}

	@Override
	public int compareTo(StringKey that) {
		return this.name.compareTo(that.name);
	}
}

package edu.osu.sfal.data;

public interface SfalDao {
	public Object lookup(String key);

	public void save(String key, Object value);
}

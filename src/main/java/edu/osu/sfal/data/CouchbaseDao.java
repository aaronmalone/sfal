package edu.osu.sfal.data;

import com.couchbase.client.CouchbaseClientIF;

public class CouchbaseDao implements SfalDao {

	private final CouchbaseClientIF couchbaseClient;

	public CouchbaseDao(CouchbaseClientIF couchbaseClient) {
		this.couchbaseClient = couchbaseClient;
	}

	@Override
	public Object lookup(String key) {
		return couchbaseClient.get(key);
	}

	@Override
	public void save(String key, Object value) {
		couchbaseClient.set(key, value);
	}
}

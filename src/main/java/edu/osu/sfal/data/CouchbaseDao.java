package edu.osu.sfal.data;

import com.couchbase.client.CouchbaseClientIF;
import org.apache.log4j.Logger;

public class CouchbaseDao implements SfalDao {

	private final Logger logger = Logger.getLogger(getClass());

	private final CouchbaseClientIF couchbaseClient;

	public CouchbaseDao(CouchbaseClientIF couchbaseClient) {
		this.couchbaseClient = couchbaseClient;
	}

	@Override
	public Object lookup(String key) {
		logger.debug("Looking up " + key);
		return couchbaseClient.get(key);
	}

	@Override
	public void save(String key, Object value) {
		logger.debug("Saving " + key + " value of type (" + value.getClass().getSimpleName() + ")");
		couchbaseClient.set(key, value);
	}
}

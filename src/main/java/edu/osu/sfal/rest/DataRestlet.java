package edu.osu.sfal.rest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.osu.sfal.data.SfalDao;
import edu.osu.sfal.data.SfalDaoInMemoryImpl;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.MediaType;

import java.util.Map;

public class DataRestlet extends Restlet {

	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private final SfalDao sfalDao;

	public DataRestlet(SfalDao sfalDao) {
		this.sfalDao = sfalDao;
	}

	@Override
	public void handle(Request request, Response response) {
		super.handle(request, response);
		String key = getKeyAttribute(request);
		if(key != null) {
			returnDataForOneKey(key, response);
		} else {
			returnAllData(response);
		}
	}

	private void returnDataForOneKey(String key, Response response) {
		Object value = sfalDao.lookup(key);
		response.setEntity(gson.toJson(value), MediaType.APPLICATION_JSON);
	}

	private void returnAllData(Response response) {
		SfalDaoInMemoryImpl inMemoryImpl = SfalDaoInMemoryImpl.class.cast(sfalDao);
		Map<String, Object> mappings = inMemoryImpl.getAllMappings();
		response.setEntity(gson.toJson(mappings), MediaType.APPLICATION_JSON);
	}

	private String getKeyAttribute(Request request) {
		return AttributeUtil.getAttribute(request, "dataStoreKey", String.class);
	}
}

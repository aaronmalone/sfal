package edu.osu.sfal.data;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SfalDaoInMemoryImpl implements SfalDao {

	private Map<String, Object> map = new ConcurrentHashMap<>();
	private List<String> keys = Collections.synchronizedList(new ArrayList<>());

	@Override
	public void save(String key, Object value) {
		keys.add(key);
		map.put(key, value);
	}

	@Override
	public Object lookup(String key) {
		return map.get(key);
	}

	public Map<String, Object> getAllMappings() {
		List<String> ks = new ArrayList<>();
		synchronized (keys) {
			for (String key : keys) {
				ks.add(key);
			}
		}
		Map<String, Object> returnMap = new HashMap<>();
		ks.forEach(k -> returnMap.put(k, map.get(k)));
		return returnMap;
	}
}

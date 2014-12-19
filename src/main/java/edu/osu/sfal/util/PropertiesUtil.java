package edu.osu.sfal.util;

import com.google.common.io.Resources;

import java.net.URL;
import java.util.Properties;

public class PropertiesUtil {
	public static Properties getPropertiesFromResource(String resource) {
		try {
			URL url = Resources.getResource(resource);
			Resources.newInputStreamSupplier(url);
			Properties properties = new Properties();
			properties.load(Resources.newInputStreamSupplier(url).getInput());
			return properties;
		} catch (Exception e) {
			if (e instanceof RuntimeException) {
				throw (RuntimeException) e;
			} else {
				throw new RuntimeException("Error loading properties.", e);
			}
		}
	}
}

package edu.osu.sfal;

import com.google.common.io.Files;
import edu.osu.sfal.util.SfalConfiguration;
import org.apache.commons.lang3.Validate;
import org.restlet.Component;
import org.restlet.data.Protocol;
import org.restlet.routing.Router;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Properties;

public class Main {

	static {
		//set this system property so we can route Restlet logging through slf4j
		System.setProperty("org.restlet.engine.loggerFacadeClass",
				"org.restlet.ext.slf4j.Slf4jLoggerFacade");
		org.apache.log4j.helpers.LogLog.setInternalDebugging(true);
	}

	public static void main(String[] args) throws Exception {
		Properties properties = getProperties(args);
		int port = getPort(properties);

		SfalConfiguration sfalConfiguration = new SfalConfiguration(properties);
		Router router = sfalConfiguration.getRouter();

		Component component = new Component();
		component.getServers().add(Protocol.HTTP, port);
		component.getDefaultHost().attachDefault(router);
		System.out.println("about to start...");
		component.start();
		System.out.println("started.");
	}

	private static Properties getProperties(String[] propertyFilesNames) throws IOException {
		Validate.notEmpty(propertyFilesNames, "properties files names array is empty");
		Properties properties = new Properties();
		for(String propsFile : propertyFilesNames) {
			properties.load(Files.newReader(new File(propsFile), Charset.defaultCharset()));
		}
		return properties;
	}

	private static int getPort(Properties properties) {
		String port = properties.getProperty("sfal.port");
		if(port == null || port.isEmpty()) {
			throw new IllegalStateException("Property 'sfal.port' is missing.");
		}
		return Integer.parseInt(port);
	}
}

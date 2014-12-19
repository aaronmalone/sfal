package edu.osu.sfal;

import com.google.common.base.Preconditions;
import edu.osu.sfal.util.PropertiesUtil;
import edu.osu.sfal.util.SfalConfiguration;
import org.restlet.Component;
import org.restlet.data.Protocol;
import org.restlet.routing.Router;

import java.util.Properties;

public class Main {

	static {
		//set this system property so we can route Restlet logging through slf4j
		System.setProperty("org.restlet.engine.loggerFacadeClass",
				"org.restlet.ext.slf4j.Slf4jLoggerFacade");
		org.apache.log4j.helpers.LogLog.setInternalDebugging(true);
	}

	public static void main(String[] args) throws Exception {
		Preconditions.checkArgument(args.length > 0, "Pass the name of a properties file.");
		Properties properties = PropertiesUtil.getPropertiesFromResource(args[0]);
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

	private static int getPort(Properties properties) {
		String port = properties.getProperty("sfal.port");
		if (port == null || port.isEmpty()) {
			throw new IllegalStateException("Property 'sfal.port' is missing.");
		}
		return Integer.parseInt(port);
	}
}

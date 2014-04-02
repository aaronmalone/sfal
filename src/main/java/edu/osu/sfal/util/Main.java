package edu.osu.sfal.util;

import org.apache.log4j.BasicConfigurator;
import org.restlet.Component;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Protocol;
import org.restlet.routing.Filter;
import org.restlet.routing.Router;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class Main {

	static {
		//set this system property so we can route Restlet logging through slf4j
		System.setProperty("org.restlet.engine.loggerFacadeClass",
				"org.restlet.ext.slf4j.Slf4jLoggerFacade");
		System.setProperty("log4j.configuration", "src/test/resources/log4j.properties");//"C:\\Users\\Aaron\\Dropbox\\Code\\sfal\\src\\test\\resources\\log4j.properties");
//		loadProperties();
		org.apache.log4j.helpers.LogLog.setInternalDebugging(true);
		BasicConfigurator.configure();
	}

	public static void main(String[] args) throws Exception {


		ApplicationContext context = new AnnotationConfigApplicationContext(SfalConfiguration.class);
		Router router = context.getBean(Router.class);

		Component component = new Component();
		component.getServers().add(Protocol.HTTP, 2345);
		component.getDefaultHost().attachDefault(router);
		System.out.println("about to start...");
		component.start();
		System.out.println("started.");
	}

	private static void loadProperties() {
		//because getting logging to work in an IDE is wildly more difficult than it should be
		Properties props = new Properties();
		try {
			props.load(new FileReader(new File("src/test/resources/log4j.properties")));
			props.forEach((key, value) -> System.setProperty((String)key, (String)value));
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}
}

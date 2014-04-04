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
}

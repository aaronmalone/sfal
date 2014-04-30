package edu.osu.sfal.util;

import org.restlet.Component;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.Protocol;
import org.restlet.routing.Router;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Main {

	static {
		//set this system property so we can route Restlet logging through slf4j
		System.setProperty("org.restlet.engine.loggerFacadeClass",
				"org.restlet.ext.slf4j.Slf4jLoggerFacade");
		org.apache.log4j.helpers.LogLog.setInternalDebugging(true);
	}

	public static void main(String[] args) throws Exception {
		ApplicationContext context = new AnnotationConfigApplicationContext(SfalConfiguration.class);
		Router router = context.getBean(Router.class);

		Component component = new Component();
		component.getServers().add(Protocol.HTTP, 2345);
		component.getDefaultHost().attachDefault(getExceptionPrintingRestlet(router));
		System.out.println("about to start...");
		component.start();
		System.out.println("started.");
	}

	private static Restlet getExceptionPrintingRestlet(final Restlet target) {
		return new Restlet() {
			@Override
			public void handle(Request request, Response response) {
				super.handle(request, response);
				try {
					target.handle(request, response);
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		};
	}
}

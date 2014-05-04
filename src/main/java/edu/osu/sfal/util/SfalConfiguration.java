package edu.osu.sfal.util;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import edu.osu.lapis.LapisApi;
import edu.osu.lapis.network.NetworkChangeCallback;
import edu.osu.sfal.actors.SfpGeneralManager;
import edu.osu.sfal.data.SfalDao;
import edu.osu.sfal.data.SfalDaoInMemoryImpl;
import edu.osu.sfal.messages.SfApplicationRequest;
import edu.osu.sfal.messages.sfp.SfpStatusMessage;
import edu.osu.sfal.rest.CacheRestlet;
import edu.osu.sfal.rest.IncomingRequestRestlet;
import edu.osu.sfal.rest.JsonEntityExtractor;
import edu.osu.sfal.rest.JsonEntityPairsExtractor;
import org.restlet.Restlet;
import org.restlet.routing.Router;
import org.restlet.routing.Validator;

import java.util.Properties;

public class SfalConfiguration {

	private final String nodeName;
	private final String coordinatorAddress;
	private final long nodeReadyTimeoutMillis;
	private final long requestCompletedTimeout;
	private final LapisApi lapisApi;
	private final Router router;

	public SfalConfiguration(Properties properties) {
		this.nodeName = getProperty(properties, "sfal.nodeName");
		this.coordinatorAddress = getProperty(properties, "sfal.network.coordinatorAddress");
		this.nodeReadyTimeoutMillis = Long.parseLong(getProperty(properties, "sfal.network.nodeReadyTimeoutMillis"));
		this.requestCompletedTimeout = Long.parseLong(getProperty(properties, "sfal.requestCompletedTimeout"));
		this.lapisApi = new LapisApi(nodeName, coordinatorAddress);

		ActorSystem system = ActorSystem.create("SFAL");
		Props props = Props.create(SfpGeneralManager.class, lapisApi);
		ActorRef actorRef = system.actorOf(props, "SfpGeneralManager");
		MessageDispatcher<SfpStatusMessage> statusDispatcher = new ActorRefMessageDispatcher<>(actorRef);
		NetworkChangeCallback ncc = new SfalLapisNetworkCallback(lapisApi, nodeReadyTimeoutMillis, statusDispatcher);
		lapisApi.registerNetworkChangeCallback(ncc);

		SfalDao sfalDao = new SfalDaoInMemoryImpl();
		CacheRestlet cacheRestlet = new CacheRestlet(sfalDao);
		this.router = new Router();
		router.attach("/cache", cacheRestlet);
		router.attach("/cache/{dataStoreKey}", cacheRestlet);
		MessageDispatcher<SfApplicationRequest> requestDispatcher = new ActorRefMessageDispatcher<>(actorRef);
		Restlet requestRestlet = new IncomingRequestRestlet(sfalDao, requestDispatcher, requestCompletedTimeout);
		Validator validator = new Validator();
		validator.validatePresence("model");
		validator.validatePresence("timestep");
		validator.validatePresence("inputs");
		validator.validatePresence("outputs");
		validator.setNext(requestRestlet);
		router.attach("/requests", new JsonEntityPairsExtractor(validator));
	}

	private static String getProperty(Properties properties, String propertyName) {
		String value = properties.getProperty(propertyName);
		if(value == null || value.isEmpty()) {
			throw new IllegalStateException("Property '" + propertyName + "' not provided.");
		}
		return value;
	}

	public Router getRouter() {
		return router;
	}
}

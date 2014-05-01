package edu.osu.sfal.util;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import edu.osu.lapis.LapisApi;
import edu.osu.lapis.network.NetworkChangeCallback;
import edu.osu.sfal.actors.SfpGeneralManager;
import edu.osu.sfal.data.SfalDao;
import edu.osu.sfal.data.SfalDaoInMemoryImpl;
import edu.osu.sfal.messages.sfp.SfpStatusMessage;
import edu.osu.sfal.rest.CacheRestlet;
import edu.osu.sfal.rest.IncomingRequestRestlet;
import edu.osu.sfal.rest.JsonEntityExtractor;
import org.apache.commons.lang3.Validate;
import org.restlet.Restlet;
import org.restlet.routing.Router;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SfalConfiguration {

	@Value("${sfal.nodeName}")
	private String nodeName;

	@Value("${sfal.network.coordinatorAddress}")
	private String coordinatorAddress;

	@Value("${sfal.network.nodeReadyTimeoutMillis}")
	private long nodeReadyTimeoutMillis;

	@Value("${sfal.requestCompletedTimeout}")
	private long requestCompletedTimeout;

	private LapisApi lapisApi = null;

	public SfalConfiguration() {
		Validate.notNull(nodeName, "nodeName is null");
		Validate.notNull(coordinatorAddress, "coordinatorAddress is null");
		this.lapisApi = new LapisApi(nodeName, coordinatorAddress);
	}

	@Bean
	public LapisApi getLapisApi() {
		lapisApi.registerNetworkChangeCallback(getSfalLapisNetworkCallback());
		return lapisApi;
	}

	@Bean NetworkChangeCallback getSfalLapisNetworkCallback() {
		Validate.notNull(lapisApi, "lapisApi null");
		return new SfalLapisNetworkCallback(lapisApi, nodeReadyTimeoutMillis, getMessageDispatcher());
	}

	@Bean
	public MessageDispatcher<SfpStatusMessage> getMessageDispatcher() {
		return new ActorRefMessageDispatcher<>(getGeneralManagerActorRef());
	}

	@Bean ActorRef getGeneralManagerActorRef() {
		Validate.notNull(lapisApi, "lapisApi null");
		ActorSystem system = ActorSystem.create("SFAL");
		Props props = Props.create(SfpGeneralManager.class, lapisApi);
		return system.actorOf(props, "SfpGeneralManager");
	}

	@Bean
	public Router getRouter() {
		SfalDao dao = new SfalDaoInMemoryImpl();
		CacheRestlet restlet = new CacheRestlet(dao);
		Router router = new Router();
		router.attach("/cache", restlet);
		router.attach("/cache/{dataStoreKey}", restlet);
		Restlet incomingRequestRestlet =
				new IncomingRequestRestlet(
						dao,
						new ActorRefMessageDispatcher<>(getGeneralManagerActorRef()),
						requestCompletedTimeout);
		router.attach("/requests", new JsonEntityExtractor(incomingRequestRestlet));
		return router;
	}

}

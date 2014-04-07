package edu.osu.sfal.util;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import edu.osu.lapis.LapisApi;
import edu.osu.lapis.network.NetworkChangeCallback;
import edu.osu.sfal.actors.*;
import edu.osu.sfal.data.SfalDao;
import edu.osu.sfal.data.SfalDaoInMemoryImpl;
import edu.osu.sfal.rest.IncomingRequestRestlet;
import edu.osu.sfal.rest.JsonEntityExtractor;
import edu.osu.sfal.rest.ThrowawayCacheRestlet;
import org.apache.commons.lang3.Validate;
import org.restlet.Restlet;
import org.restlet.routing.Router;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SfalConfiguration {

	//TODO: THIS CLASS NEEDS SIGNIFICANT CLEAN-UP

	private ActorRef generalManagerActorRef = null;

	@Bean
	public LapisApi getLapisApi() {
		LapisApi lapisApi = new LapisApi(getNodeName(), getCoordinatorAddress());
		NetworkChangeCallback callback = new SfalLapisNetworkCallback(lapisApi,
				getNodeReadyTimeout(), getDispatcherToSfpGeneralManager(lapisApi));
		lapisApi.registerNetworkChangeCallback(callback);
		return lapisApi;
	}

	private String getNodeName() {
		return "SFAL"; //TODO MAKE CONFIGURABLE
	}

	private String getCoordinatorAddress() {
		return "http://127.0.0.1:22333"; //TODO MAKE CONFIGURABLE
	}

	private long getNodeReadyTimeout() {
		return 15000; //TODO MAKE CONFIGURABLE
	}

	@Bean
	public Router getRouter() {
		SfalDao dao = new SfalDaoInMemoryImpl();
		ThrowawayCacheRestlet restlet = new ThrowawayCacheRestlet(dao);
		Router router = new Router();
		router.attach("/cache", restlet);
		router.attach("/cache/{dataStoreKey}", restlet);
		Validate.notNull(generalManagerActorRef);
		ActorRefMessageDispatcher a = new ActorRefMessageDispatcher<>(generalManagerActorRef);
		Restlet incomingRequestRestlet = new IncomingRequestRestlet(dao, a, getRequestCompletedTimeout());
		router.attach("/requests", new JsonEntityExtractor(null, incomingRequestRestlet));
		return router;
	}

	public <T> MessageDispatcher<T> getDispatcherToSfpGeneralManager(LapisApi lapisApi) {
		Validate.notNull(lapisApi);
		ActorSystem system = ActorSystem.create("SFAL");
		PropsFactory<SfpActor> sfpActorPropsFac = new SfpActorPropsFactory(lapisApi);
		PropsFactory<SfpPoolManager> sfpPoolManagerPropsFactory = new SfpPoolManagerPropsFactory(sfpActorPropsFac);
		Props props = Props.create(SfpGeneralManager.class, sfpPoolManagerPropsFactory);
		generalManagerActorRef =  system.actorOf(props, "SfpGeneralManager");
		return new ActorRefMessageDispatcher<>(generalManagerActorRef);
	}

	public long getRequestCompletedTimeout() {
		return 5000; //TODO MAKE CONFIGURABLE
	}
}

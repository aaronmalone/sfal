package edu.osu.sfal.util;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import edu.osu.lapis.LapisApi;
import edu.osu.lapis.network.NetworkChangeCallback;
import edu.osu.sfal.actors.SfpGeneralManager;
import edu.osu.sfal.data.SfalDao;
import edu.osu.sfal.data.SfalDaoInMemoryImpl;
import edu.osu.sfal.rest.IncomingRequestRestlet;
import edu.osu.sfal.rest.JsonEntityExtractor;
import edu.osu.sfal.rest.ThrowawayCacheRestlet;
import org.restlet.routing.Router;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class SfalConfiguration {

	private LapisApi lapisApi;

	@Bean
	public LapisApi getLapisApi() {
		lapisApi = new LapisApi(getNodeName(), getCoordinatorAddress());
		NetworkChangeCallback callback = new SfalLapisNetworkCallback(lapisApi,
				getNodeReadyTimeout(), getDispatcherToSfpGeneralManager());
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
		return 25000; //TODO MAKE CONFIGURABLE
	}

	@Bean
	public Router getRouter() {
		Router router = new Router();
		router.attach("/cache", getThrowawayCacheRestlet());
		router.attach("/cache/{dataStoreKey}", getThrowawayCacheRestlet());
		router.attach("/requests", getJsonEntityExtractor());
		return router;
	}

	@Bean
	public ThrowawayCacheRestlet getThrowawayCacheRestlet() {
		return new ThrowawayCacheRestlet(getSfalDao());
	}

	@Bean
	public JsonEntityExtractor getJsonEntityExtractor() {
		return new JsonEntityExtractor(null, getIncomingRequestRestlet());
	}

	@Bean
	public IncomingRequestRestlet getIncomingRequestRestlet() {
		return new IncomingRequestRestlet(getSfalDao(), getDispatcherToSfpGeneralManager(), getTimeout());
	}

	@Bean
	@Scope("prototype")
	public <T> MessageDispatcher<T> getDispatcherToSfpGeneralManager() {
		return new ActorRefMessageDispatcher<>(getSfpGeneralManagerActorRef());
	}

	@Bean
	public ActorRef getSfpGeneralManagerActorRef() {
		ActorSystem system = getActorSystem();
		Props props = Props.create(SfpGeneralManager.class, (Object[]) null); //TODO NOT CORRECT - FIX
		return system.actorOf(props, "SfpGeneralManager");
	}

	@Bean
	public ActorSystem getActorSystem() {
		return  ActorSystem.create("SFAL");
	}

	@Bean
	public SfalDao getSfalDao() {
		return new SfalDaoInMemoryImpl();
	}

	@Bean
	public long getTimeout() {
		return 5000; //TODO MAKE CONFIGURABLE
	}
}

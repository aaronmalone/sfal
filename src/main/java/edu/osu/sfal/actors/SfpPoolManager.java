package edu.osu.sfal.actors;

import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import akka.actor.ActorRef;
import com.google.common.collect.Maps;
import edu.osu.sfal.messages.SfpActorRegistration;
import edu.osu.sfal.util.SfpName;
import edu.osu.sfal.util.SimulationFunctionName;
import org.apache.commons.lang3.Validate;
import scala.concurrent.Promise;
import akka.actor.UntypedActor;
import edu.osu.sfal.messages.SfApplication;
import edu.osu.sfal.messages.SfApplicationRequest;
import edu.osu.sfal.messages.SfApplicationResult;

public class SfpPoolManager extends UntypedActor {

    private final boolean BUSY = true, NOT_BUSY = false;

    private SimulationFunctionName simulationFunctionName;

	final Map<SfApplication, Promise<SfApplicationResult>> resultPromiseMap = createResultPromiseMap();
	final Queue<SfApplication> sfApplicationQueue = new LinkedList<>();
    final Map<SfpName, ActorRef> sfpActorMap = Maps.newHashMap();
    final Map<SfpName, Boolean> sfpBusyMap = Maps.newHashMap();

    @Override
	public void onReceive(Object message) throws Exception {
        if(message instanceof SimulationFunctionName) {
            setSimulationFunctionName((SimulationFunctionName) message);
        } else if(message instanceof SfpActorRegistration) {
            handleSfpActorRegistration((SfpActorRegistration) message);
        } else if(message instanceof SfApplicationRequest) {
            handleSfApplicationRequest((SfApplicationRequest) message);
        } else if(message instanceof SfApplicationResult) {
            handleSfApplicationResult((SfApplicationResult) message);
        } else {
            unhandled(message);
        }
	}

    private void handleSfpActorRegistration(SfpActorRegistration reg) {
        validateSimFunctionName(reg.getSimulationFunctionName());
        sfpActorMap.put(reg.getSfpName(), reg.getActorRef());
        setActorBusyness(reg.getSfpName(), NOT_BUSY);
    }

    /**
     * Handles the simulation function application request.
     */
    private void handleSfApplicationRequest(SfApplicationRequest sfApplicationRequest) {
        validateSimFunctionName(sfApplicationRequest.getSfApplication());
        storeInResultPromiseMap(sfApplicationRequest);
        attemptToDispatchRequest(sfApplicationRequest);
    }

    private void storeInResultPromiseMap(SfApplicationRequest sfApplicationRequest) {
        SfApplication sfApp = sfApplicationRequest.getSfApplication();
        Promise<SfApplicationResult> resultPromise = sfApplicationRequest.getSfApplicationResultPromise();
        resultPromiseMap.put(sfApp, resultPromise);
    }

    /**
     * Dispatches request to an SfpActor if any are not busy. If all are busy, enqueues request.
     */
    private void attemptToDispatchRequest(SfApplicationRequest sfApplicationRequest) {
        if(isPoolEmptyOfSFPs()) {
            handlePoolEmptyOfSFPs(sfApplicationRequest);
        } else if(allPooledSFPsAreBusy()) {
            enqueueRequest(sfApplicationRequest);
        } else {
            dispatchToSfp(sfApplicationRequest);
        }
    }

    private boolean isPoolEmptyOfSFPs() {
        return sfpActorMap.isEmpty();
    }

    private void handlePoolEmptyOfSFPs(SfApplicationRequest sfApplicationRequest) {
        throw new IllegalStateException("Received SfApplicationRequest when pool of SFPs is empty.");
    }

    private boolean allPooledSFPsAreBusy() {
        Validate.isTrue(!sfpBusyMap.isEmpty());
        for(boolean busy : sfpBusyMap.values()) {
            if(!busy) {
                return false;
            }
        }
        return true;
    }

    private void enqueueRequest(SfApplicationRequest sfApplicationRequest) {
        SfApplication sfa = sfApplicationRequest.getSfApplication();
        sfApplicationQueue.add(sfa);
    }

	/**
	 * Succeeds the corresponding Promise and removes the SfApplication from 
	 * resultPromiseMap.
	 */
	private void handleSfApplicationResult(SfApplicationResult sfApplicationResult) {
        validateSimFunctionName(sfApplicationResult.getSfApplication());
		succeedCorrespondingPromise(sfApplicationResult);
		removeFromResultPromiseMap(sfApplicationResult);
        //TODO one method to handle actor state
		if(sfApplicationQueue.isEmpty()) {
			//SfpActor is no longer busy
            sfpBusyMap.put(sfApplicationResult.getSfpName(), NOT_BUSY);
            setActorBusyness(sfApplicationResult.getSfpName(), NOT_BUSY);
		} else {
			//send queued request back to SfpActor
            SfApplication queued = sfApplicationQueue.poll();
            getSender().tell(queued, getSelf());
		}
	}

	private void succeedCorrespondingPromise(SfApplicationResult sfApplicationResult) {
		SfApplication sfApp = sfApplicationResult.getSfApplication();
		Promise<SfApplicationResult> resultPromise = resultPromiseMap.get(sfApp);
		resultPromise.success(sfApplicationResult);
	}
	
	private void removeFromResultPromiseMap(SfApplicationResult sfApplicationResult) {
		resultPromiseMap.remove(sfApplicationResult.getSfApplication());
	}

	private void dispatchToSfp(SfApplicationRequest sfApplicationRequest) {
		// may want to implement preferred order in the future.. or now we just grab the first one
        for(Map.Entry<SfpName,Boolean> entry : sfpBusyMap.entrySet()) {
            SfpName sfp = entry.getKey();
            boolean busy = entry.getValue();
            if(!busy) {
                dispatchToSpecificSfp(sfp, sfApplicationRequest);
                entry.setValue(BUSY);
                return;
            }
        }
	}

    private void dispatchToSpecificSfp(SfpName sfp, SfApplicationRequest sfApplicationRequest) {
        SfApplication sfApp = sfApplicationRequest.getSfApplication();
        sfpActorMap.get(sfp).tell(sfApp, getSelf());
    }

    private void setActorBusyness(SfpName sfpName, boolean busy) {
        this.sfpBusyMap.put(sfpName, busy);
    }

    private void setSimulationFunctionName(SimulationFunctionName simulationFunctionName) {
        this.simulationFunctionName = simulationFunctionName;
    }

    private void validateSimFunctionName(SfApplication sfApplication) {
        validateSimFunctionName(sfApplication.getSimulationFunctionName());
    }

    private void validateSimFunctionName(SimulationFunctionName simFuncName) {
        Validate.isTrue(this.simulationFunctionName.equals(simFuncName));
    }

    private Map<SfApplication, Promise<SfApplicationResult>> createResultPromiseMap() {
		/* Yes, we're using an IdentityHashMap. The alternative is defining 
		 * hashCode() and equals() on SimulationFunctionApplication, and I don't
		 * care to spend my time on that, so this will work just as well.
		 */
		return new IdentityHashMap<>();
	}
}

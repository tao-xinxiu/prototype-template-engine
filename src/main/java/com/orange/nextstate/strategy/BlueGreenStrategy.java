package com.orange.nextstate.strategy;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.model.StrategyConfig;
import com.orange.model.state.Architecture;
import com.orange.model.state.ArchitectureMicroservice;
import com.orange.util.SetUtil;

public class BlueGreenStrategy extends Strategy {
    private static final Logger logger = LoggerFactory.getLogger(BlueGreenStrategy.class);

    public BlueGreenStrategy(StrategyConfig config) {
	super(config);
    }

    @Override
    public boolean valid(Architecture currentState, Architecture finalState) {
	// TODO in the case of multi new versions, version should be specified
	// in finalState
	return true;
    }

    @Override
    public List<Transit> transits() {
	return Arrays.asList(newPkgEnvTransit, updateExceptRouteTransit, library.updateRouteTransit,
		library.removeUndesiredTransit);
    }

    /**
     * next architecture: add microservice with new pkg and env. Tagging
     * updating microservice with version "updating"
     */
    protected Transit newPkgEnvTransit = new Transit() {
	@Override
	public Architecture next(Architecture currentState, Architecture finalState) {
	    Architecture nextState = new Architecture(currentState);
	    for (String site : finalState.listSitesName()) {
		Set<ArchitectureMicroservice> currentMicroservices = nextState.getArchitectureSite(site)
			.getArchitectureMicroservices();
		for (ArchitectureMicroservice desiredMicroservice : finalState.getArchitectureSite(site)
			.getArchitectureMicroservices()) {
		    if (SetUtil.noneMatch(currentMicroservices, ms -> ms.getName().equals(desiredMicroservice.getName())
			    && ms.getVersion().equals(library.desiredVersion(desiredMicroservice)))) {
			ArchitectureMicroservice newMicroservice = new ArchitectureMicroservice(desiredMicroservice);
			newMicroservice.setGuid(null);
			newMicroservice.setRoutes(library.tmpRoute(site, desiredMicroservice));
			if (newMicroservice.getVersion() == null) {
			    newMicroservice.setVersion(config.getUpdatingVersion());
			}
			nextState.getArchitectureSite(site).addArchitectureMicroservice(newMicroservice);
			logger.info("Added a new microservice: {} ", newMicroservice);
			continue;
		    }
		}
	    }
	    return nextState;
	}
    };

    /**
     * getting next architecture by updating desired microservice properties
     * (except route)
     */
    protected Transit updateExceptRouteTransit = new Transit() {
	// assume that it doesn't exist two microservices with same pkg and name
	@Override
	public Architecture next(Architecture currentState, Architecture finalState) {
	    Architecture nextState = new Architecture(currentState);
	    for (String site : finalState.listSitesName()) {
		for (ArchitectureMicroservice desiredMicroservice : finalState.getArchitectureSite(site)
			.getArchitectureMicroservices()) {
		    if (SetUtil.noneMatch(nextState.getArchitectureSite(site).getArchitectureMicroservices(),
			    ms -> ms.getName().equals(desiredMicroservice.getName())
				    && ms.getVersion().equals(library.desiredVersion(desiredMicroservice))
				    && ms.getPath().equals(desiredMicroservice.getPath())
				    && ms.getEnv().equals(desiredMicroservice.getEnv())
				    && ms.getNbProcesses() == desiredMicroservice.getNbProcesses()
				    && ms.getServices().equals(desiredMicroservice.getServices())
				    && ms.getState().equals(desiredMicroservice.getState()))) {
			ArchitectureMicroservice nextMicroservice = SetUtil.getOneMicroservice(
				nextState.getArchitectureSite(site).getArchitectureMicroservices(),
				ms -> ms.getName().equals(desiredMicroservice.getName())
					&& ms.getVersion().equals(library.desiredVersion(desiredMicroservice)));
			nextMicroservice.setPath(desiredMicroservice.getPath());
			nextMicroservice.setEnv(desiredMicroservice.getEnv());
			nextMicroservice.setNbProcesses(desiredMicroservice.getNbProcesses());
			nextMicroservice.setServices(desiredMicroservice.getServices());
			nextMicroservice.setState(desiredMicroservice.getState());
			logger.info("Updated microservice [{}_{}] to {} ", nextMicroservice.getName(),
				nextMicroservice.getVersion(), nextMicroservice);
		    }
		}
	    }
	    return nextState;
	}
    };
}

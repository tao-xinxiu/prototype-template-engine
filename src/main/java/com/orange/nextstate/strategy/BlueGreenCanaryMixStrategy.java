package com.orange.nextstate.strategy;

import java.util.Arrays;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.model.StrategyConfig;
import com.orange.model.state.Architecture;
import com.orange.model.state.ArchitectureMicroservice;
import com.orange.util.SetUtil;

public class BlueGreenCanaryMixStrategy extends TagUpdatingVersionStrategy {
    private static final Logger logger = LoggerFactory.getLogger(BlueGreenCanaryMixStrategy.class);

    public BlueGreenCanaryMixStrategy(StrategyConfig config) {
	super(config);
	transitions = Arrays.asList(addCanaryTransit, updateExceptInstancesRoutesTransit, updateRouteTransit, rolloutTransit,
		library.removeUndesiredTransit);
    }

    @Override
    public boolean valid(Architecture currentState, Architecture finalState) {
	return true;
    }

    /**
     * next architecture: add canary microservice with new pkg and env
     */
    protected Transition addCanaryTransit = new Transition() {
	@Override
	public Architecture next(Architecture currentState, Architecture finalState) {
	    Architecture nextState = new Architecture(currentState);
	    for (String site : finalState.listSitesName()) {
		Set<ArchitectureMicroservice> currentMicroservices = nextState.getArchitectureMicroservices(site);
		for (ArchitectureMicroservice desiredMicroservice : finalState.getArchitectureMicroservices(site)) {
		    if (SetUtil.noneMatch(currentMicroservices,
			    ms -> ms.getName().equals(desiredMicroservice.getName())
				    && ms.getPath().equals(desiredMicroservice.getPath())
				    && ms.getEnv().equals(desiredMicroservice.getEnv()))) {
			ArchitectureMicroservice newMicroservice = new ArchitectureMicroservice(desiredMicroservice);
			newMicroservice.setGuid(null);
			if (newMicroservice.getVersion() == null) {
			    newMicroservice.setVersion(config.getUpdatingVersion());
			}
			newMicroservice.setRoutes(library.tmpRoute(site, desiredMicroservice));
			newMicroservice.setNbProcesses(config.getCanaryNbr());
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
     * next architecture: update desired microservice properties except
     * nbrProcesses and routes
     */
    protected Transition updateExceptInstancesRoutesTransit = new Transition() {
	// assume that it doesn't exist two microservices with same pkg and name
	@Override
	public Architecture next(Architecture currentState, Architecture finalState) {
	    Architecture nextState = new Architecture(currentState);
	    for (String site : finalState.listSitesName()) {
		for (ArchitectureMicroservice desiredMicroservice : finalState.getArchitectureMicroservices(site)) {
		    if (SetUtil.noneMatch(nextState.getArchitectureMicroservices(site),
			    ms -> ms.getName().equals(desiredMicroservice.getName())
				    && ms.getVersion().equals(config.getUpdatingVersion())
				    && ms.getPath().equals(desiredMicroservice.getPath())
				    && ms.getEnv().equals(desiredMicroservice.getEnv())
				    && ms.getServices().equals(desiredMicroservice.getServices())
				    && ms.getState().equals(desiredMicroservice.getState()))) {
			ArchitectureMicroservice nextMicroservice = SetUtil.getUniqueMicroservice(
				nextState.getArchitectureMicroservices(site),
				ms -> ms.getName().equals(desiredMicroservice.getName())
					&& ms.getVersion().equals(config.getUpdatingVersion()));
			nextMicroservice.setPath(desiredMicroservice.getPath());
			nextMicroservice.setEnv(desiredMicroservice.getEnv());
			nextMicroservice.setServices(desiredMicroservice.getServices());
			nextMicroservice.setState(desiredMicroservice.getState());
			nextMicroservice.setRoutes(library.tmpRoute(site, desiredMicroservice));
			logger.info("Updated microservice [{}_{}] to {} ", nextMicroservice.getName(),
				nextMicroservice.getVersion(), nextMicroservice);
		    }
		}
	    }
	    return nextState;
	}
    };

    /**
     * next architecture: update desired microservice route
     */
    protected Transition updateRouteTransit = new Transition() {
	// assume that it doesn't exist two microservices with same pkg and name
	@Override
	public Architecture next(Architecture currentState, Architecture finalState) {
	    Architecture nextState = new Architecture(currentState);
	    for (String site : finalState.listSitesName()) {
		for (ArchitectureMicroservice desiredMicroservice : finalState.getArchitectureMicroservices(site)) {
		    if (SetUtil.noneMatch(nextState.getArchitectureMicroservices(site),
			    ms -> ms.getName().equals(desiredMicroservice.getName())
				    && ms.getVersion().equals(config.getUpdatingVersion())
				    && ms.getPath().equals(desiredMicroservice.getPath())
				    && ms.getEnv().equals(desiredMicroservice.getEnv())
				    && ms.getServices().equals(desiredMicroservice.getServices())
				    && ms.getState().equals(desiredMicroservice.getState())
				    && ms.getRoutes().equals(desiredMicroservice.getRoutes()))) {
			ArchitectureMicroservice nextMicroservice = SetUtil.getUniqueMicroservice(
				nextState.getArchitectureMicroservices(site),
				ms -> ms.getName().equals(desiredMicroservice.getName())
					&& ms.getVersion().equals(config.getUpdatingVersion()));
			nextMicroservice.setRoutes(desiredMicroservice.getRoutes());
			logger.info("Updated microservice [{}_{}] route to {} ", nextMicroservice.getName(),
				nextMicroservice.getVersion(), nextMicroservice.getRoutes());
		    }
		}
	    }
	    return nextState;
	}
    };

    /**
     * next architecture: scale up desired microservice and rollout old ones
     */
    protected Transition rolloutTransit = new Transition() {
	@Override
	public Architecture next(Architecture currentState, Architecture finalState) {
	    Architecture nextState = new Architecture(currentState);
	    for (String site : finalState.listSitesName()) {
		for (ArchitectureMicroservice desiredMicroservice : finalState.getArchitectureMicroservices(site)) {
		    Set<ArchitectureMicroservice> nextMicroservices = nextState.getArchitectureMicroservices(site);
		    if (SetUtil.noneMatch(nextMicroservices, ms -> ms.isInstantiation(desiredMicroservice))) {
			for (ArchitectureMicroservice nextMicroservice : SetUtil.searchByName(nextMicroservices,
				desiredMicroservice.getName())) {
			    if (nextMicroservice.getVersion().equals(library.desiredVersion(desiredMicroservice))) {
				int nextNbr = nextMicroservice.getNbProcesses() + config.getCanaryIncrease();
				if (nextNbr > desiredMicroservice.getNbProcesses()) {
				    nextNbr = desiredMicroservice.getNbProcesses();
				}
				nextMicroservice.setNbProcesses(nextNbr);
			    } else {
				int nextNbr = nextMicroservice.getNbProcesses() - config.getCanaryIncrease();
				if (nextNbr < 1) {
				    nextNbr = 1;
				}
				nextMicroservice.setNbProcesses(nextNbr);
			    }
			}
		    }
		}
	    }
	    return nextState;
	}
    };

}

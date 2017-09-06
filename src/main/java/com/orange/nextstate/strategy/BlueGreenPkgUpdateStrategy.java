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

public class BlueGreenPkgUpdateStrategy extends TagUpdatingVersionStrategy {
    private static final Logger logger = LoggerFactory.getLogger(BlueGreenPkgUpdateStrategy.class);

    public BlueGreenPkgUpdateStrategy(StrategyConfig config) {
	super(config);
    }

    @Override
    public boolean valid(Architecture currentState, Architecture finalState) {
	return true;
    }

    @Override
    public List<Transit> transits() {
	return Arrays.asList(newPkgTransit, updateExceptPkgTransit, library.removeUndesiredTransit);
    }

    /**
     * getting next architecture by adding microservice with new pkg
     */
    protected Transit newPkgTransit = new Transit() {
	@Override
	public Architecture next(Architecture currentState, Architecture finalState) {
	    Architecture nextState = new Architecture(currentState);
	    for (String site : finalState.listSitesName()) {
		for (ArchitectureMicroservice desiredMicroservice : finalState.getArchitectureMicroservices(site)) {
		    if (SetUtil.search(currentState.getArchitectureMicroservices(site),
			    ms -> ms.getName().equals(desiredMicroservice.getName())
				    && ms.getPath().equals(desiredMicroservice.getPath()))
			    .isEmpty()) {
			ArchitectureMicroservice newMicroservice = new ArchitectureMicroservice(desiredMicroservice);
			newMicroservice.setGuid(null);
			newMicroservice.setRoutes(library.tmpRoute(site, desiredMicroservice));
			if (newMicroservice.getVersion() == null) {
			    newMicroservice.setVersion(config.getUpdatingVersion());
			}
			nextState.getArchitectureSite(site).addArchitectureMicroservice(newMicroservice);
			logger.info("Added a new microservice: {} ", newMicroservice);
		    }
		}
	    }
	    return nextState;
	}
    };

    /**
     * getting next architecture by updating desired microservice properties
     */
    protected Transit updateExceptPkgTransit = new Transit() {
	// assume that it doesn't exist two microservices with same pkg and name
	@Override
	public Architecture next(Architecture currentState, Architecture finalState) {
	    Architecture nextState = new Architecture(currentState);
	    for (String site : finalState.listSitesName()) {
		for (ArchitectureMicroservice desiredMicroservice : finalState.getArchitectureMicroservices(site)) {
		    if (SetUtil.noneMatch(nextState.getArchitectureMicroservices(site),
			    ms -> isInstantiation(ms, desiredMicroservice))) {
			Set<ArchitectureMicroservice> nextMicroservices = nextState.getArchitectureMicroservices(site);
			ArchitectureMicroservice nextMicroservice = SetUtil.getOneMicroservice(nextMicroservices,
				ms -> ms.getName().equals(desiredMicroservice.getName())
					&& ms.getPath().equals(desiredMicroservice.getPath()));
			nextMicroservice.setRoutes(desiredMicroservice.getRoutes());
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

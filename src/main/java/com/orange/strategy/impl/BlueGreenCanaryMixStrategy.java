package com.orange.strategy.impl;

import java.util.Arrays;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.model.StrategyConfig;
import com.orange.model.architecture.Architecture;
import com.orange.model.architecture.Microservice;
import com.orange.strategy.TagUpdatingVersionStrategy;
import com.orange.strategy.Transition;
import com.orange.util.SetUtil;

public class BlueGreenCanaryMixStrategy extends TagUpdatingVersionStrategy {
    private static final Logger logger = LoggerFactory.getLogger(BlueGreenCanaryMixStrategy.class);

    public BlueGreenCanaryMixStrategy(StrategyConfig config) {
	super(config);
	transitions = Arrays.asList(addCanaryTransit, updateExceptInstancesRoutesTransit, updateRouteTransit, rolloutTransit,
		library.removeUndesiredTransit);
    }

    @Override
    public boolean valid(Architecture currentArchitecture, Architecture finalArchitecture) {
	return true;
    }

    /**
     * next architecture: add canary microservice with new pkg and env
     */
    protected Transition addCanaryTransit = new Transition() {
	@Override
	public Architecture next(Architecture currentArchitecture, Architecture finalArchitecture) {
	    Architecture nextArchitecture = new Architecture(currentArchitecture);
	    for (String site : finalArchitecture.listSitesName()) {
		Set<Microservice> currentMicroservices = nextArchitecture.getSiteMicroservices(site);
		for (Microservice desiredMicroservice : finalArchitecture.getSiteMicroservices(site)) {
		    if (SetUtil.noneMatch(currentMicroservices,
			    ms -> ms.getName().equals(desiredMicroservice.getName())
				    && ms.getPath().equals(desiredMicroservice.getPath())
				    && ms.getEnv().equals(desiredMicroservice.getEnv()))) {
			Microservice newMicroservice = new Microservice(desiredMicroservice);
			newMicroservice.setGuid(null);
			if (newMicroservice.getVersion() == null) {
			    newMicroservice.setVersion(config.getUpdatingVersion());
			}
			newMicroservice.setRoutes(library.tmpRoute(site, desiredMicroservice));
			newMicroservice.setNbProcesses(config.getCanaryNbr());
			nextArchitecture.getSite(site).addMicroservice(newMicroservice);
			logger.info("Added a new microservice: {} ", newMicroservice);
			continue;
		    }
		}
	    }
	    return nextArchitecture;
	}
    };

    /**
     * next architecture: update desired microservice properties except
     * nbrProcesses and routes
     */
    protected Transition updateExceptInstancesRoutesTransit = new Transition() {
	// assume that it doesn't exist two microservices with same pkg and name
	@Override
	public Architecture next(Architecture currentArchitecture, Architecture finalArchitecture) {
	    Architecture nextArchitecture = new Architecture(currentArchitecture);
	    for (String site : finalArchitecture.listSitesName()) {
		for (Microservice desiredMicroservice : finalArchitecture.getSiteMicroservices(site)) {
		    if (SetUtil.noneMatch(nextArchitecture.getSiteMicroservices(site),
			    ms -> ms.getName().equals(desiredMicroservice.getName())
				    && ms.getVersion().equals(config.getUpdatingVersion())
				    && ms.getPath().equals(desiredMicroservice.getPath())
				    && ms.getEnv().equals(desiredMicroservice.getEnv())
				    && ms.getServices().equals(desiredMicroservice.getServices())
				    && ms.getState().equals(desiredMicroservice.getState()))) {
			Microservice nextMicroservice = SetUtil.getUniqueMicroservice(
				nextArchitecture.getSiteMicroservices(site),
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
	    return nextArchitecture;
	}
    };

    /**
     * next architecture: update desired microservice route
     */
    protected Transition updateRouteTransit = new Transition() {
	// assume that it doesn't exist two microservices with same pkg and name
	@Override
	public Architecture next(Architecture currentArchitecture, Architecture finalArchitecture) {
	    Architecture nextArchitecture = new Architecture(currentArchitecture);
	    for (String site : finalArchitecture.listSitesName()) {
		for (Microservice desiredMicroservice : finalArchitecture.getSiteMicroservices(site)) {
		    if (SetUtil.noneMatch(nextArchitecture.getSiteMicroservices(site),
			    ms -> ms.getName().equals(desiredMicroservice.getName())
				    && ms.getVersion().equals(config.getUpdatingVersion())
				    && ms.getPath().equals(desiredMicroservice.getPath())
				    && ms.getEnv().equals(desiredMicroservice.getEnv())
				    && ms.getServices().equals(desiredMicroservice.getServices())
				    && ms.getState().equals(desiredMicroservice.getState())
				    && ms.getRoutes().equals(desiredMicroservice.getRoutes()))) {
			Microservice nextMicroservice = SetUtil.getUniqueMicroservice(
				nextArchitecture.getSiteMicroservices(site),
				ms -> ms.getName().equals(desiredMicroservice.getName())
					&& ms.getVersion().equals(config.getUpdatingVersion()));
			nextMicroservice.setRoutes(desiredMicroservice.getRoutes());
			logger.info("Updated microservice [{}_{}] route to {} ", nextMicroservice.getName(),
				nextMicroservice.getVersion(), nextMicroservice.getRoutes());
		    }
		}
	    }
	    return nextArchitecture;
	}
    };

    /**
     * next architecture: scale up desired microservice and rollout old ones
     */
    protected Transition rolloutTransit = new Transition() {
	@Override
	public Architecture next(Architecture currentArchitecture, Architecture finalArchitecture) {
	    Architecture nextArchitecture = new Architecture(currentArchitecture);
	    for (String site : finalArchitecture.listSitesName()) {
		for (Microservice desiredMicroservice : finalArchitecture.getSiteMicroservices(site)) {
		    Set<Microservice> nextMicroservices = nextArchitecture.getSiteMicroservices(site);
		    if (SetUtil.noneMatch(nextMicroservices, ms -> ms.isInstantiation(desiredMicroservice))) {
			for (Microservice nextMicroservice : SetUtil.searchByName(nextMicroservices,
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
	    return nextArchitecture;
	}
    };

}

package com.orange.strategy.impl;

import java.util.Arrays;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.model.StrategyConfig;
import com.orange.model.architecture.Architecture;
import com.orange.model.architecture.ArchitectureMicroservice;
import com.orange.strategy.TagUpdatingVersionStrategy;
import com.orange.strategy.Transition;
import com.orange.util.SetUtil;

public class BlueGreenStrategy extends TagUpdatingVersionStrategy {
    private static final Logger logger = LoggerFactory.getLogger(BlueGreenStrategy.class);

    public BlueGreenStrategy(StrategyConfig config) {
	super(config);
	transitions = Arrays.asList(newPkgEnvTransit, updateExceptRouteTransit, library.updateRouteTransit,
		library.removeUndesiredTransit);
    }

    @Override
    public boolean valid(Architecture currentArchitecture, Architecture finalArchitecture) {
	// TODO in the case of multi new versions, version should be specified
	// (i.e. not null) in finalArchitecture
	return true;
    }

    /**
     * next architecture: add microservice with new pkg and env. Tagging
     * updating microservice with version "updating"
     */
    protected Transition newPkgEnvTransit = new Transition() {
	@Override
	public Architecture next(Architecture currentArchitecture, Architecture finalArchitecture) {
	    Architecture nextArchitecture = new Architecture(currentArchitecture);
	    for (String site : finalArchitecture.listSitesName()) {
		Set<ArchitectureMicroservice> currentMicroservices = nextArchitecture
			.getSiteMicroservices(site);
		for (ArchitectureMicroservice desiredMicroservice : finalArchitecture
			.getSiteMicroservices(site)) {
		    if (SetUtil.noneMatch(currentMicroservices, ms -> ms.getName().equals(desiredMicroservice.getName())
			    && ms.getVersion().equals(library.desiredVersion(desiredMicroservice)))) {
			ArchitectureMicroservice newMicroservice = new ArchitectureMicroservice(desiredMicroservice);
			newMicroservice.setGuid(null);
			newMicroservice.setRoutes(library.tmpRoute(site, desiredMicroservice));
			if (newMicroservice.getVersion() == null) {
			    newMicroservice.setVersion(config.getUpdatingVersion());
			}
			nextArchitecture.getArchitectureSite(site).addArchitectureMicroservice(newMicroservice);
			logger.info("Added a new microservice: {} ", newMicroservice);
			continue;
		    }
		}
	    }
	    return nextArchitecture;
	}
    };

    /**
     * getting next architecture by updating desired microservice properties
     * (except route)
     */
    protected Transition updateExceptRouteTransit = new Transition() {
	// assume that it doesn't exist two microservices with same pkg and name
	@Override
	public Architecture next(Architecture currentArchitecture, Architecture finalArchitecture) {
	    Architecture nextArchitecture = new Architecture(currentArchitecture);
	    for (String site : finalArchitecture.listSitesName()) {
		for (ArchitectureMicroservice desiredMicroservice : finalArchitecture
			.getSiteMicroservices(site)) {
		    if (SetUtil.noneMatch(nextArchitecture.getSiteMicroservices(site),
			    ms -> ms.getName().equals(desiredMicroservice.getName())
				    && ms.getVersion().equals(library.desiredVersion(desiredMicroservice))
				    && ms.getPath().equals(desiredMicroservice.getPath())
				    && ms.getEnv().equals(desiredMicroservice.getEnv())
				    && ms.getNbProcesses() == desiredMicroservice.getNbProcesses()
				    && ms.getServices().equals(desiredMicroservice.getServices())
				    && ms.getState().equals(desiredMicroservice.getState()))) {
			ArchitectureMicroservice nextMicroservice = SetUtil.getOneMicroservice(
				nextArchitecture.getSiteMicroservices(site),
				ms -> ms.getName().equals(desiredMicroservice.getName())
					&& ms.getVersion().equals(library.desiredVersion(desiredMicroservice)));
			nextMicroservice.setPath(desiredMicroservice.getPath());
			nextMicroservice.setEnv(desiredMicroservice.getEnv());
			nextMicroservice.setNbProcesses(desiredMicroservice.getNbProcesses());
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
}

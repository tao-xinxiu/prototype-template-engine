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

public class BlueGreenPkgUpdateStrategy extends TagUpdatingVersionStrategy {
    private static final Logger logger = LoggerFactory.getLogger(BlueGreenPkgUpdateStrategy.class);

    public BlueGreenPkgUpdateStrategy(StrategyConfig config) {
	super(config);
	transitions = Arrays.asList(newPkgTransit, updateExceptPkgTransit, library.removeUndesiredTransit);
    }

    @Override
    public boolean valid(Architecture currentArchitecture, Architecture finalArchitecture) {
	return true;
    }

    /**
     * getting next architecture by adding microservice with new pkg
     */
    protected Transition newPkgTransit = new Transition() {
	@Override
	public Architecture next(Architecture currentArchitecture, Architecture finalArchitecture) {
	    Architecture nextArchitecture = new Architecture(currentArchitecture);
	    for (String site : finalArchitecture.listSitesName()) {
		for (ArchitectureMicroservice desiredMicroservice : finalArchitecture.getSiteMicroservices(site)) {
		    if (SetUtil.search(currentArchitecture.getSiteMicroservices(site),
			    ms -> ms.getName().equals(desiredMicroservice.getName())
				    && ms.getPath().equals(desiredMicroservice.getPath()))
			    .isEmpty()) {
			ArchitectureMicroservice newMicroservice = new ArchitectureMicroservice(desiredMicroservice);
			newMicroservice.setGuid(null);
			newMicroservice.setRoutes(library.tmpRoute(site, desiredMicroservice));
			if (newMicroservice.getVersion() == null) {
			    newMicroservice.setVersion(config.getUpdatingVersion());
			}
			nextArchitecture.getArchitectureSite(site).addArchitectureMicroservice(newMicroservice);
			logger.info("Added a new microservice: {} ", newMicroservice);
		    }
		}
	    }
	    return nextArchitecture;
	}
    };

    /**
     * getting next architecture by updating desired microservice properties
     */
    protected Transition updateExceptPkgTransit = new Transition() {
	// assume that it doesn't exist two microservices with same pkg and name
	@Override
	public Architecture next(Architecture currentArchitecture, Architecture finalArchitecture) {
	    Architecture nextArchitecture = new Architecture(currentArchitecture);
	    for (String site : finalArchitecture.listSitesName()) {
		for (ArchitectureMicroservice desiredMicroservice : finalArchitecture.getSiteMicroservices(site)) {
		    if (SetUtil.noneMatch(nextArchitecture.getSiteMicroservices(site),
			    ms -> ms.isInstantiation(desiredMicroservice))) {
			Set<ArchitectureMicroservice> nextMicroservices = nextArchitecture.getSiteMicroservices(site);
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
	    return nextArchitecture;
	}
    };
}

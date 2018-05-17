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

public class BlueGreenStrategy extends TagUpdatingVersionStrategy {
    private static final Logger logger = LoggerFactory.getLogger(BlueGreenStrategy.class);

    public BlueGreenStrategy(StrategyConfig config) {
	super(config);
	transitions = Arrays.asList(newPkgEnvTransit, updateExceptRouteTransit, library.updateRouteTransit,
		library.removeUndesiredTransit);
    }

    @Override
    public boolean valid(Architecture currentArchitecture, Architecture finalArchitecture) {
	// TODO- in the case of multi new versions, version should be specified
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
		Set<Microservice> currentMicroservices = nextArchitecture.getSiteMicroservices(site);
		for (Microservice desiredMicroservice : finalArchitecture.getSiteMicroservices(site)) {
		    if (SetUtil.noneMatch(currentMicroservices, ms -> ms.get("name").equals(desiredMicroservice.get("name"))
			    && ms.get("version").equals(library.desiredVersion(desiredMicroservice)))) {
			Microservice newMicroservice = new Microservice(desiredMicroservice);
			newMicroservice.set("guid", null);
			newMicroservice.set("routes", library.tmpRoute(site, desiredMicroservice));
			if (newMicroservice.get("version") == null) {
			    newMicroservice.set("version", config.getUpdatingVersion());
			}
			nextArchitecture.getSite(site).addMicroservice(newMicroservice);
			logger.info("Added a new microservice deployment: {} ", newMicroservice);
			// continue;
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
		for (Microservice desiredMicroservice : finalArchitecture.getSiteMicroservices(site)) {
		    if (SetUtil.noneMatch(nextArchitecture.getSiteMicroservices(site),
			    ms -> ms.get("name").equals(desiredMicroservice.get("name"))
				    && ms.get("version").equals(library.desiredVersion(desiredMicroservice))
				    && ms.get("path").equals(desiredMicroservice.get("path"))
				    && ms.get("env").equals(desiredMicroservice.get("env"))
				    && ms.get("nbProcesses") == desiredMicroservice.get("nbProcesses")
				    && ms.get("services").equals(desiredMicroservice.get("services"))
				    && ms.get("state").equals(desiredMicroservice.get("state")))) {
			Microservice nextMicroservice = SetUtil.getOneMicroservice(
				nextArchitecture.getSiteMicroservices(site),
				ms -> ms.get("name").equals(desiredMicroservice.get("name"))
					&& ms.get("version").equals(library.desiredVersion(desiredMicroservice)));
			nextMicroservice.set("path", desiredMicroservice.get("path"));
			nextMicroservice.set("env", desiredMicroservice.get("env"));
			nextMicroservice.set("nbProcesses", desiredMicroservice.get("nbProcesses"));
			nextMicroservice.set("services", desiredMicroservice.get("services"));
			nextMicroservice.set("state", desiredMicroservice.get("state"));
			nextMicroservice.set("routes", library.tmpRoute(site, desiredMicroservice));
			logger.info("Updated microservice [{}_{}] to {} ", nextMicroservice.get("name"),
				nextMicroservice.get("version"), nextMicroservice);
		    }
		}
	    }
	    return nextArchitecture;
	}
    };
}

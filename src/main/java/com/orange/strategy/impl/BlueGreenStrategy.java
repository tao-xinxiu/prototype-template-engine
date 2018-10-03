package com.orange.strategy.impl;

import java.util.Arrays;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.model.StrategyConfig;
import com.orange.model.architecture.Architecture;
import com.orange.model.architecture.Microservice;
import com.orange.strategy.Strategy;
import com.orange.strategy.Transition;
import com.orange.util.SetUtil;

public class BlueGreenStrategy extends Strategy {
    private static final Logger logger = LoggerFactory.getLogger(BlueGreenStrategy.class);

    public BlueGreenStrategy(StrategyConfig config) {
	super(config);
	transitions = Arrays.asList(newPkgEnvTransit, updateExceptRouteTransit,
		library.updateRouteTransit(Arrays.asList("guid")), library.removeUndesiredTransit);
    }

    @Override
    public boolean valid(Architecture currentArchitecture, Architecture finalArchitecture) {
	// TODO- in the case of multi new versions, version should be specified
	// (i.e. not null) in finalArchitecture
	return true;
    }

    /**
     * next architecture: add microservice with new pkg and env. Tagging updating
     * microservice with version "updating"
     */
    protected Transition newPkgEnvTransit = new Transition() {
	@Override
	public Architecture next(Architecture currentArchitecture, Architecture finalArchitecture) {
	    Architecture nextArchitecture = new Architecture(currentArchitecture);
	    for (String site : finalArchitecture.listSitesName()) {
		Set<Microservice> currentMicroservices = nextArchitecture.getSiteMicroservices(site);
		for (Microservice desiredMs : finalArchitecture.getSiteMicroservices(site)) {
		    Microservice updatingMs = SetUtil.getUniqueMicroservice(currentMicroservices,
			    (String) desiredMs.get("name"), (String) desiredMs.get("version"));
		    if (updatingMs == null && SetUtil.noneMatch(currentMicroservices,
			    ms -> ms.eqAttr(Arrays.asList("name", "path", "env"), desiredMs))) {
			Microservice newMs = new Microservice(desiredMs);
			newMs.set("guid", null);
			newMs.set("routes", library.tmpRoute(site, desiredMs));
			nextArchitecture.getSite(site).addMicroservice(newMs);
			logger.info("Added a new microservice: {} ", newMs);
		    }
		    continue;
		}
	    }
	    return nextArchitecture;
	}
    };

    /**
     * getting next architecture by updating desired microservice properties (except
     * route)
     */
    protected Transition updateExceptRouteTransit = new Transition() {
	// assume that it doesn't exist two microservices with same pkg and name
	@Override
	public Architecture next(Architecture currentArchitecture, Architecture finalArchitecture) {
	    Architecture nextArchitecture = new Architecture(currentArchitecture);
	    for (String site : finalArchitecture.listSitesName()) {
		for (Microservice desiredMs : finalArchitecture.getSiteMicroservices(site)) {
		    Microservice nextMs = SetUtil.getUniqueMicroservice(nextArchitecture.getSiteMicroservices(site),
			    (String) desiredMs.get("name"), (String) desiredMs.get("version"));
		    if (nextMs != null && !nextMs.eqAttrExcept(Arrays.asList("guid", "routes"), desiredMs)) {
			nextMs.copyAttrExcept(Arrays.asList("guid", "routes"), desiredMs);
			nextMs.set("routes", library.tmpRoute(site, desiredMs));
			logger.info("Updated microservice [{}_{}] to {} ", nextMs.get("name"), nextMs.get("version"),
				nextMs);
		    }
		}
	    }
	    return nextArchitecture;
	}
    };
}

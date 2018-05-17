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
		for (Microservice desiredMicroservice : finalArchitecture.getSiteMicroservices(site)) {
		    if (SetUtil.search(currentArchitecture.getSiteMicroservices(site),
			    ms -> ms.get("name").equals(desiredMicroservice.get("name"))
				    && ms.get("path").equals(desiredMicroservice.get("path")))
			    .isEmpty()) {
			Microservice newMicroservice = new Microservice(desiredMicroservice);
			newMicroservice.set("guid", null);
			newMicroservice.set("routes", library.tmpRoute(site, desiredMicroservice));
			if (newMicroservice.get("version") == null) {
			    newMicroservice.set("version", config.getUpdatingVersion());
			}
			nextArchitecture.getSite(site).addMicroservice(newMicroservice);
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
		for (Microservice desiredMicroservice : finalArchitecture.getSiteMicroservices(site)) {
		    if (SetUtil.noneMatch(nextArchitecture.getSiteMicroservices(site),
			    ms -> ms.isInstantiation(desiredMicroservice))) {
			Set<Microservice> nextMicroservices = nextArchitecture.getSiteMicroservices(site);
			Microservice nextMicroservice = SetUtil.getOneMicroservice(nextMicroservices,
				ms -> ms.get("name").equals(desiredMicroservice.get("name"))
					&& ms.get("path").equals(desiredMicroservice.get("path")));
			nextMicroservice.set("routes", desiredMicroservice.get("routes"));
			nextMicroservice.set("env", desiredMicroservice.get("env"));
			nextMicroservice.set("nbProcesses", desiredMicroservice.get("nbProcesses"));
			nextMicroservice.set("services", desiredMicroservice.get("services"));
			nextMicroservice.set("state", desiredMicroservice.get("state"));
			logger.info("Updated microservice [{}_{}] to {} ", nextMicroservice.get("name"),
				nextMicroservice.get("version"), nextMicroservice);
		    }
		}
	    }
	    return nextArchitecture;
	}
    };
}

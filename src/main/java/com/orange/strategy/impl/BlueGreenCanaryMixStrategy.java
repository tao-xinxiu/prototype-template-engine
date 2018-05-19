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
	transitions = Arrays.asList(addCanaryTransit, updateExceptInstancesRoutesTransit, updateRouteTransit,
		rolloutTransit, library.removeUndesiredTransit);
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
			    ms -> ms.eqAttr(Arrays.asList("name", "path", "env"), desiredMicroservice))) {
			Microservice updatingMs = SetUtil.getUniqueMicroservice(currentMicroservices,
				(String) desiredMicroservice.get("name"), config.getUpdatingVersion());
			if (updatingMs != null) {
			    // copy all properties except id, version
			    Microservice newMicroservice = new Microservice(desiredMicroservice);
			    newMicroservice.copyAttr(Arrays.asList("guid", "version"), updatingMs);
			    updatingMs = newMicroservice;
			} else {
			    Microservice newMicroservice = new Microservice(desiredMicroservice);
			    newMicroservice.set("guid", null);
			    if (newMicroservice.get("version") == null) {
				newMicroservice.set("version", config.getUpdatingVersion());
			    }
			    newMicroservice.set("routes", library.tmpRoute(site, desiredMicroservice));
			    newMicroservice.set("nbProcesses", config.getCanaryNbr());
			    nextArchitecture.getSite(site).addMicroservice(newMicroservice);
			    logger.info("Added a new microservice: {} ", newMicroservice);
			}
			continue;
		    }
		}
	    }
	    return nextArchitecture;
	}
    };

    /**
     * next architecture: update desired microservice properties except nbrProcesses
     * and routes
     */
    protected Transition updateExceptInstancesRoutesTransit = new Transition() {
	// assume that it doesn't exist two microservices with same pkg and name
	@Override
	public Architecture next(Architecture currentArchitecture, Architecture finalArchitecture) {
	    Architecture nextArchitecture = new Architecture(currentArchitecture);
	    for (String site : finalArchitecture.listSitesName()) {
		for (Microservice desiredMs : finalArchitecture.getSiteMicroservices(site)) {
		    if (SetUtil.noneMatch(nextArchitecture.getSiteMicroservices(site),
			    ms -> ms.eqAttr(Arrays.asList("name", "path", "env", "services", "state"), desiredMs)
				    && ms.get("version").equals(config.getUpdatingVersion()))) {
			Microservice nextMicroservice = SetUtil.getUniqueMicroservice(
				nextArchitecture.getSiteMicroservices(site), ms -> ms.eqAttr("name", desiredMs)
					&& ms.get("version").equals(config.getUpdatingVersion()));
			nextMicroservice.copyAttr(Arrays.asList("path", "env", "services", "state"), desiredMs);
			nextMicroservice.set("routes", library.tmpRoute(site, desiredMs));
			logger.info("Updated microservice [{}_{}] to {} ", nextMicroservice.get("name"),
				nextMicroservice.get("version"), nextMicroservice);
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
		for (Microservice desiredMs : finalArchitecture.getSiteMicroservices(site)) {
		    if (SetUtil.noneMatch(nextArchitecture.getSiteMicroservices(site),
			    ms -> ms.eqAttr(Arrays.asList("name", "path", "env", "services", "state", "routes"),
				    desiredMs) && ms.get("version").equals(config.getUpdatingVersion()))) {
			Microservice nextMicroservice = SetUtil.getUniqueMicroservice(
				nextArchitecture.getSiteMicroservices(site), ms -> ms.eqAttr("name", desiredMs)
					&& ms.get("version").equals(config.getUpdatingVersion()));
			nextMicroservice.copyAttr("routes", desiredMs);
			logger.info("Updated microservice [{}_{}] route to {} ", nextMicroservice.get("name"),
				nextMicroservice.get("version"), nextMicroservice.get("routes"));
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
				(String) desiredMicroservice.get("name"))) {
			    if (nextMicroservice.get("version").equals(library.desiredVersion(desiredMicroservice))) {
				int nextNbr = (int) nextMicroservice.get("nbProcesses") + config.getCanaryIncrease();
				if (nextNbr > (int) desiredMicroservice.get("nbProcesses")) {
				    nextNbr = (int) desiredMicroservice.get("nbProcesses");
				}
				nextMicroservice.set("nbProcesses", nextNbr);
			    } else {
				int nextNbr = (int) nextMicroservice.get("nbProcesses") - config.getCanaryIncrease();
				if (nextNbr < 1) {
				    nextNbr = 1;
				}
				nextMicroservice.set("nbProcesses", nextNbr);
			    }
			}
		    }
		}
	    }
	    return nextArchitecture;
	}
    };
}

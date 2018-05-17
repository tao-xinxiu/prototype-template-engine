package com.orange.strategy.impl;

import java.util.Arrays;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.model.StrategyConfig;
import com.orange.model.architecture.Architecture;
import com.orange.model.architecture.Microservice;
import com.orange.model.architecture.MicroserviceState;
import com.orange.strategy.Transition;
import com.orange.util.SetUtil;

// Strategy assume route not updated between Ainit and Af
public class CanaryStrategy extends BlueGreenCanaryMixStrategy {
    private static final Logger logger = LoggerFactory.getLogger(CanaryStrategy.class);

    public CanaryStrategy(StrategyConfig config) {
	super(config);
	transitions = Arrays.asList(rolloutTransit, addCanaryTransit, updateExceptInstancesRoutesTransit,
		updateRouteTransit, scaleupTransit, library.removeUndesiredTransit);
    }

    @Override
    public boolean valid(Architecture currentArchitecture, Architecture finalArchitecture) {
	for (String site : finalArchitecture.listSitesName()) {
	    for (Microservice desiredMicroservice : finalArchitecture.getSiteMicroservices(site)) {
		if (desiredMicroservice.get("state") != MicroserviceState.RUNNING) {
		    return false;
		}
		Set<Microservice> currentMicroservices = SetUtil.searchByName(
			currentArchitecture.getSiteMicroservices(site), (String) desiredMicroservice.get("name"));
		if (!SetUtil.uniqueByPathEnv(currentMicroservices)) {
		    return false;
		}
	    }
	}
	return true;
    }

    /**
     * next architecture: scale down non-desired microservice when the
     * microservice routed running instances equals to desired instances
     */
    protected Transition rolloutTransit = new Transition() {
	@Override
	public Architecture next(Architecture currentArchitecture, Architecture finalArchitecture) {
	    Architecture nextArchitecture = new Architecture(currentArchitecture);
	    for (String site : finalArchitecture.listSitesName()) {
		for (Microservice desiredMicroservice : finalArchitecture.getSiteMicroservices(site)) {
		    Set<Microservice> nextMicroservices = nextArchitecture.getSiteMicroservices(site);
		    Set<Microservice> relatedMicroservices = SetUtil.search(nextMicroservices,
			    ms -> ms.get("name").equals(desiredMicroservice.get("name"))
				    && ms.get("state").equals(desiredMicroservice.get("state"))
				    && ms.get("routes").equals(desiredMicroservice.get("routes")));
		    if (relatedMicroservices.stream().mapToInt(ms -> (int) ms.get("nbProcesses"))
			    .sum() == (int) desiredMicroservice.get("nbProcesses")) {
			Microservice nextMicroservice = SetUtil.getUniqueMicroservice(relatedMicroservices,
				ms -> !ms.get("version").equals(library.desiredVersion(desiredMicroservice)));
			boolean canaryNotCreated = SetUtil.noneMatch(relatedMicroservices,
				ms -> ms.get("version").equals(library.desiredVersion(desiredMicroservice)));
			int scaleDownNb = canaryNotCreated ? config.getCanaryNbr() : config.getCanaryIncrease();
			int nextNbr = (int) nextMicroservice.get("nbProcesses") - scaleDownNb;
			if (nextNbr >= 1) {
			    nextMicroservice.set("nbProcesses", nextNbr);
			    logger.info("rolled out microservice {}", nextMicroservice);
			} else {
			    nextMicroservices.remove(nextMicroservice);
			    logger.info("removed microservice {}", nextMicroservice);
			}
		    }
		}
	    }
	    return nextArchitecture;
	}
    };

    /**
     * get next architecture: scale up desired microservices
     */
    protected Transition scaleupTransit = new Transition() {
	@Override
	public Architecture next(Architecture currentArchitecture, Architecture finalArchitecture) {
	    Architecture nextArchitecture = new Architecture(currentArchitecture);
	    for (String site : finalArchitecture.listSitesName()) {
		for (Microservice desiredMicroservice : finalArchitecture.getSiteMicroservices(site)) {
		    Set<Microservice> nextMicroservices = nextArchitecture.getSiteMicroservices(site);
		    if (SetUtil.noneMatch(nextMicroservices, ms -> ms.isInstantiation(desiredMicroservice))) {
			Microservice nextMicroservice = SetUtil.getUniqueMicroservice(nextMicroservices,
				ms -> ms.get("name").equals(desiredMicroservice.get("name"))
					&& ms.get("version").equals(library.desiredVersion(desiredMicroservice)));
			int nextNbr = (int) nextMicroservice.get("nbProcesses") + config.getCanaryIncrease();
			if (nextNbr > (int) desiredMicroservice.get("nbProcesses")) {
			    nextNbr = (int) desiredMicroservice.get("nbProcesses");
			}
			nextMicroservice.set("nbProcesses", nextNbr);
		    }
		}
	    }
	    return nextArchitecture;
	}

    };
}

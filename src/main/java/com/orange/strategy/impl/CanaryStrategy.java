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
		if (desiredMicroservice.getState() != MicroserviceState.RUNNING) {
		    return false;
		}
		Set<Microservice> currentMicroservices = SetUtil
			.searchByName(currentArchitecture.getSiteMicroservices(site), desiredMicroservice.getName());
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
			    ms -> ms.getName().equals(desiredMicroservice.getName())
				    && ms.getState().equals(desiredMicroservice.getState())
				    && ms.getRoutes().equals(desiredMicroservice.getRoutes()));
		    if (relatedMicroservices.stream().mapToInt(ms -> ms.getNbProcesses()).sum() == desiredMicroservice
			    .getNbProcesses()) {
			Microservice nextMicroservice = SetUtil.getUniqueMicroservice(relatedMicroservices,
				ms -> !ms.getVersion().equals(library.desiredVersion(desiredMicroservice)));
			boolean canaryNotCreated = SetUtil.noneMatch(relatedMicroservices,
				ms -> ms.getVersion().equals(library.desiredVersion(desiredMicroservice)));
			int scaleDownNb = canaryNotCreated ? config.getCanaryNbr() : config.getCanaryIncrease();
			int nextNbr = nextMicroservice.getNbProcesses() - scaleDownNb;
			if (nextNbr >= 1) {
			    nextMicroservice.setNbProcesses(nextNbr);
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
				ms -> ms.getName().equals(desiredMicroservice.getName())
					&& ms.getVersion().equals(library.desiredVersion(desiredMicroservice)));
			int nextNbr = nextMicroservice.getNbProcesses() + config.getCanaryIncrease();
			if (nextNbr > desiredMicroservice.getNbProcesses()) {
			    nextNbr = desiredMicroservice.getNbProcesses();
			}
			nextMicroservice.setNbProcesses(nextNbr);
		    }
		}
	    }
	    return nextArchitecture;
	}

    };
}

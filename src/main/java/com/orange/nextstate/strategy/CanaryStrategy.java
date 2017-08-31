package com.orange.nextstate.strategy;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.model.StrategyConfig;
import com.orange.model.state.MicroserviceState;
import com.orange.model.state.Architecture;
import com.orange.model.state.ArchitectureMicroservice;
import com.orange.util.SetUtil;

// Strategy assume route not updated between Ainit and Af
public class CanaryStrategy extends BlueGreenCanaryMixStrategy {
    private static final Logger logger = LoggerFactory.getLogger(CanaryStrategy.class);

    public CanaryStrategy(StrategyConfig config) {
	super(config);
    }

    @Override
    public boolean valid(Architecture currentState, Architecture finalState) {
	for (String site : finalState.listSitesName()) {
	    for (ArchitectureMicroservice desiredMicroservice : finalState.getArchitectureSite(site)
		    .getArchitectureMicroservices()) {
		if (desiredMicroservice.getState() != MicroserviceState.RUNNING) {
		    return false;
		}
		Set<ArchitectureMicroservice> currentMicroservices = SetUtil.searchByName(
			currentState.getArchitectureSite(site).getArchitectureMicroservices(),
			desiredMicroservice.getName());
		if (!SetUtil.uniqueByPathEnv(currentMicroservices)) {
		    return false;
		}
	    }
	}
	return true;
    }

    @Override
    public List<Transit> transits() {
	return Arrays.asList(rolloutTransit, addCanaryTransit, updateExceptInstancesRoutesTransit, updateRouteTransit,
		scaleupTransit, library.removeUndesiredTransit);
    }

    /**
     * next architecture: scale down non-desired microservice when the
     * microservice routed running instances equals to desired instances
     */
    protected Transit rolloutTransit = new Transit() {
	@Override
	public Architecture next(Architecture currentState, Architecture finalState) {
	    Architecture nextState = new Architecture(currentState);
	    for (String site : finalState.listSitesName()) {
		for (ArchitectureMicroservice desiredMicroservice : finalState.getArchitectureSite(site)
			.getArchitectureMicroservices()) {
		    Set<ArchitectureMicroservice> nextMicroservices = nextState.getArchitectureSite(site)
			    .getArchitectureMicroservices();
		    Set<ArchitectureMicroservice> relatedMicroservices = SetUtil.search(nextMicroservices,
			    ms -> ms.getName().equals(desiredMicroservice.getName())
				    && ms.getState().equals(desiredMicroservice.getState())
				    && ms.getRoutes().equals(desiredMicroservice.getRoutes()));
		    if (relatedMicroservices.stream().mapToInt(ms -> ms.getNbProcesses())
			    .sum() == desiredMicroservice.getNbProcesses()) {
			ArchitectureMicroservice nextMicroservice = SetUtil.getUniqueMicroservice(relatedMicroservices,
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
	    return nextState;
	}
    };

    /**
     * get next architecture: scale up desired microservices
     */
    protected Transit scaleupTransit = new Transit() {
	@Override
	public Architecture next(Architecture currentState, Architecture finalState) {
	    Architecture nextState = new Architecture(currentState);
	    for (String site : finalState.listSitesName()) {
		for (ArchitectureMicroservice desiredMicroservice : finalState.getArchitectureSite(site)
			.getArchitectureMicroservices()) {
		    Set<ArchitectureMicroservice> nextMicroservices = nextState.getArchitectureSite(site)
			    .getArchitectureMicroservices();
		    if (SetUtil.noneMatch(nextMicroservices, ms -> ms.isInstantiation(desiredMicroservice))) {
			ArchitectureMicroservice nextMicroservice = SetUtil.getUniqueMicroservice(nextMicroservices,
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
	    return nextState;
	}

    };
}
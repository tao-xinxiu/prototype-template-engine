package com.orange.nextstate.strategy;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.model.StrategyConfig;
import com.orange.model.state.MicroserviceState;
import com.orange.model.state.Architecture;
import com.orange.model.state.ArchitectureMicroservice;
import com.orange.util.SetUtil;
import com.orange.util.VersionGenerator;

public class StrategyLibrary {
    private static final Logger logger = LoggerFactory.getLogger(StrategyLibrary.class);

    private StrategyConfig config;

    public StrategyLibrary(StrategyConfig config) {
	this.config = config;
    }

    /**
     * next architecture: remove micro-services not in finalState
     */
    protected Transit removeUndesiredTransit = new Transit() {
	@Override
	public Architecture next(Architecture currentState, Architecture finalState) {
	    Architecture nextState = new Architecture(currentState);
	    for (String site : finalState.listSitesName()) {
		Set<ArchitectureMicroservice> nextMicroservices = nextState.getArchitectureSite(site)
			.getArchitectureMicroservices();
		Iterator<ArchitectureMicroservice> iterator = nextMicroservices.iterator();
		while (iterator.hasNext()) {
		    ArchitectureMicroservice microservice = iterator.next();
		    if (SetUtil.noneMatch(finalState.getArchitectureSite(site).getArchitectureMicroservices(),
			    desiredMs -> microservice.isInstantiation(desiredMs))) {
			iterator.remove();
			logger.info("Removed microservice [{}]", microservice);
		    } else if (microservice.getVersion().equals(config.getUpdatingVersion())) {
			microservice.setVersion(VersionGenerator.random(SetUtil.collectVersions(nextMicroservices)));
		    }
		}
	    }
	    return nextState;
	}
    };

    /**
     * next architecture: adding new micro-services (in finalState, not in
     * currentState)
     */
    protected Transit addNewTransit = new Transit() {
	@Override
	public Architecture next(Architecture currentState, Architecture finalState) {
	    Architecture nextState = new Architecture(currentState);
	    for (String site : finalState.listSitesName()) {
		Set<ArchitectureMicroservice> currentMicroservices = nextState.getArchitectureSite(site)
			.getArchitectureMicroservices();
		for (ArchitectureMicroservice desiredMicroservice : finalState.getArchitectureSite(site)
			.getArchitectureMicroservices()) {
		    if (SetUtil.noneMatch(currentMicroservices,
			    ms -> ms.getName().equals(desiredMicroservice.getName()))) {
			ArchitectureMicroservice newMicroservice = new ArchitectureMicroservice(desiredMicroservice);
			newMicroservice.setGuid(null);
			newMicroservice.setVersion(VersionGenerator.random(new HashSet<>()));
			nextState.getArchitectureSite(site).addArchitectureMicroservice(newMicroservice);
			logger.info("Added a new microservice: {} ", newMicroservice);
			continue;
		    }
		}
	    }
	    return nextState;
	}
    };

    /**
     * next architecture: direct in-place update current most similar
     * microservice to desired
     * 
     * @param tmpRoute
     *            whether map new or path/env updated microservices to a
     *            temporary route
     * @return
     */
    protected Transit directTransit(boolean tmpRoute) {
	return new Transit() {
	    @Override
	    public Architecture next(Architecture currentState, Architecture finalState) {
		Architecture nextState = new Architecture(finalState);
		for (String site : finalState.listSitesName()) {
		    for (ArchitectureMicroservice nextMicroservice : nextState.getArchitectureSite(site)
			    .getArchitectureMicroservices()) {
			Set<ArchitectureMicroservice> currentMicroservices = SetUtil.searchByName(
				currentState.getArchitectureSite(site).getArchitectureMicroservices(),
				nextMicroservice.getName());
			if (currentMicroservices.size() == 0) {
			    // Add non-exist microservice
			    nextMicroservice.setGuid(null);
			    nextMicroservice.setVersion(VersionGenerator.random(new HashSet<>()));
			    if (tmpRoute) {
				nextMicroservice.setRoutes(tmpRoute(site, nextMicroservice));
			    }
			    logger.info("{} detected as a new microservice.", nextMicroservice);
			} else {
			    // update from most similar microservice (i.e.
			    // prefer path and env equals if exist)
			    ArchitectureMicroservice currentMicroservice = SetUtil.getOneMicroservice(
				    currentMicroservices, ms -> ms.getPath().equals(nextMicroservice.getPath())
					    && ms.getEnv().equals(nextMicroservice.getEnv()));
			    if (currentMicroservice == null) {
				currentMicroservice = currentMicroservices.iterator().next();
				if (tmpRoute) {
				    nextMicroservice.setRoutes(tmpRoute(site, nextMicroservice));
				}
			    }
			    nextMicroservice.setGuid(currentMicroservice.getGuid());
			    nextMicroservice.setVersion(currentMicroservice.getVersion());
			    logger.info("{} detected as a updated microservice", nextMicroservice);
			}
		    }
		}
		return nextState;
	    }
	};
    }

    /**
     * getting next architecture by updating desired microservice route and
     * setting version
     */
    protected Transit updateRouteTransit = new Transit() {
	// assume that it doesn't exist two microservices with same pkg and name
	@Override
	public Architecture next(Architecture currentState, Architecture finalState) {
	    Architecture nextState = new Architecture(currentState);
	    for (String site : finalState.listSitesName()) {
		for (ArchitectureMicroservice desiredMicroservice : finalState.getArchitectureSite(site)
			.getArchitectureMicroservices()) {
		    Set<ArchitectureMicroservice> nextMicroservices = SetUtil.searchByName(
			    nextState.getArchitectureSite(site).getArchitectureMicroservices(),
			    desiredMicroservice.getName());
		    if (SetUtil.noneMatch(nextMicroservices, ms -> ms.isInstantiation(desiredMicroservice))) {
			ArchitectureMicroservice nextMicroservice = SetUtil.getOneMicroservice(nextMicroservices,
				ms -> ms.getVersion().equals(desiredVersion(desiredMicroservice))
					&& ms.getPath().equals(desiredMicroservice.getPath())
					&& ms.getEnv().equals(desiredMicroservice.getEnv())
					&& ms.getNbProcesses() == desiredMicroservice.getNbProcesses()
					&& ms.getServices().equals(desiredMicroservice.getServices())
					&& ms.getState().equals(desiredMicroservice.getState()));
			nextMicroservice.setRoutes(desiredMicroservice.getRoutes());
			logger.info("Updated microservice [{}_{}] route to {} ", nextMicroservice.getName(),
				nextMicroservice.getVersion(), nextMicroservice.getRoutes());
		    }
		}
	    }
	    return nextState;
	}
    };

    protected Transit cleanAllTransit = new Transit() {
	@Override
	public Architecture next(Architecture currentState, Architecture finalState) {
	    Architecture nextState = new Architecture(currentState);
	    nextState.getArchitectureSites().values().stream().forEach(s -> s.getArchitectureMicroservices().clear());
	    return nextState;
	}
    };

    protected Transit deployAllTransit = new Transit() {
	@Override
	public Architecture next(Architecture currentState, Architecture finalState) {
	    Architecture nextState = new Architecture(currentState);
	    for (String site : finalState.listSitesName()) {
		Set<ArchitectureMicroservice> nextMicroservices = nextState.getArchitectureSite(site)
			.getArchitectureMicroservices();
		if (nextMicroservices.isEmpty()) {
		    nextMicroservices.addAll(finalState.getArchitectureSite(site).getArchitectureMicroservices());
		}
	    }
	    return nextState;
	}
    };

    protected Transit scaleHorizontalTransit = new Transit() {
	@Override
	public Architecture next(Architecture currentState, Architecture finalState) {
	    Architecture nextState = new Architecture(currentState);
	    for (String site : finalState.listSitesName()) {
		for (ArchitectureMicroservice desiredMicroservice : finalState.getArchitectureSite(site)
			.getArchitectureMicroservices()) {
		    ArchitectureMicroservice nextMicroservice = SetUtil.getUniqueMicroservice(
			    nextState.getArchitectureSite(site).getArchitectureMicroservices(),
			    desiredMicroservice.getName(), desiredVersion(desiredMicroservice));
		    if (nextMicroservice.getNbProcesses() != desiredMicroservice.getNbProcesses()) {
			nextMicroservice.setNbProcesses(desiredMicroservice.getNbProcesses());
			logger.info("Updated microservice [{}_{}] nbProcesses to {} ", nextMicroservice.getName(),
				nextMicroservice.getVersion(), nextMicroservice.getNbProcesses());
		    }
		}
	    }
	    return nextState;
	}
    };

    protected Transit scaleIncrementalTransit = new Transit() {
	@Override
	public Architecture next(Architecture currentState, Architecture finalState) {
	    Architecture nextState = new Architecture(currentState);
	    for (String site : finalState.listSitesName()) {
		for (ArchitectureMicroservice desiredMicroservice : finalState.getArchitectureSite(site)
			.getArchitectureMicroservices()) {
		    ArchitectureMicroservice nextMicroservice = SetUtil.getUniqueMicroservice(
			    nextState.getArchitectureSite(site).getArchitectureMicroservices(),
			    desiredMicroservice.getName(), desiredVersion(desiredMicroservice));
		    if (nextMicroservice.getNbProcesses() <= desiredMicroservice.getNbProcesses()) {
			int nextNbr = nextMicroservice.getNbProcesses() + config.getCanaryIncrease();
			nextNbr = nextNbr > desiredMicroservice.getNbProcesses() ? desiredMicroservice.getNbProcesses()
				: nextNbr;
			nextMicroservice.setNbProcesses(nextNbr);
			logger.info("Updated microservice [{}_{}] nbProcesses to {} ", nextMicroservice.getName(),
				nextMicroservice.getVersion(), nextNbr);
		    }
		}
	    }
	    return nextState;
	}
    };

    protected Transit scaleVerticalTransit = new Transit() {
	@Override
	public Architecture next(Architecture currentState, Architecture finalState) {
	    Architecture nextState = new Architecture(currentState);
	    for (String site : finalState.listSitesName()) {
		for (ArchitectureMicroservice desiredMicroservice : finalState.getArchitectureSite(site)
			.getArchitectureMicroservices()) {
		    ArchitectureMicroservice nextMicroservice = SetUtil.getUniqueMicroservice(
			    nextState.getArchitectureSite(site).getArchitectureMicroservices(),
			    desiredMicroservice.getName(), desiredVersion(desiredMicroservice));
		    if (nextMicroservice.getMemory().equals(desiredMicroservice.getMemory())) {
			nextMicroservice.setMemory(desiredMicroservice.getMemory());
			logger.info("Updated microservice [{}_{}] memory to {} ", nextMicroservice.getName(),
				nextMicroservice.getVersion(), nextMicroservice.getMemory());
		    }
		    if (nextMicroservice.getDisk().equals(desiredMicroservice.getDisk())) {
			nextMicroservice.setDisk(desiredMicroservice.getDisk());
			logger.info("Updated microservice [{}_{}] disk to {} ", nextMicroservice.getName(),
				nextMicroservice.getVersion(), nextMicroservice.getDisk());
		    }
		}
	    }
	    return nextState;
	}
    };

    protected Transit cleanNotUpdatableMicroserviceTransit = new Transit() {
	@Override
	public Architecture next(Architecture currentState, Architecture finalState) {
	    Architecture nextState = new Architecture(currentState);
	    for (String site : finalState.listSitesName()) {
		// based on path = "" if null, during getCurrentState
		Set<ArchitectureMicroservice> nextMicroservices = nextState.getArchitectureSite(site)
			.getArchitectureMicroservices();
		Set<ArchitectureMicroservice> nonUpdatableMicroservice = SetUtil.search(nextMicroservices,
			ms -> "".equals(ms.getPath()) || ms.getState().equals(MicroserviceState.CREATED));
		if (!nonUpdatableMicroservice.isEmpty()) {
		    logger.info("clean up not updatable microservices: {}", nonUpdatableMicroservice);
		    nextMicroservices.removeAll(nonUpdatableMicroservice);
		}
	    }
	    return nextState;
	}
    };

    public String desiredVersion(ArchitectureMicroservice desiredMicroservice) {
	return desiredMicroservice.getVersion() == null ? config.getUpdatingVersion()
		: desiredMicroservice.getVersion();
    }

    public Set<String> tmpRoute(String site, ArchitectureMicroservice microservice) {
	return Collections.singleton(config.getSiteConfig(site).getTmpRoute(microservice.getName()));
    }
}

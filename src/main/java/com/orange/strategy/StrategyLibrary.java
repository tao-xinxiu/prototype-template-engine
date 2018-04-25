package com.orange.strategy;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.model.StrategyConfig;
import com.orange.model.architecture.Architecture;
import com.orange.model.architecture.Microservice;
import com.orange.model.architecture.MicroserviceState;
import com.orange.util.SetUtil;
import com.orange.util.VersionGenerator;

public class StrategyLibrary {
    private Logger logger;
    private StrategyConfig config;

    public StrategyLibrary(StrategyConfig config) {
	this.logger = LoggerFactory.getLogger(StrategyLibrary.class);
	this.config = config;
    }

    /**
     * next architecture: remove micro-services not in finalArchitecture
     */
    public Transition removeUndesiredTransit = new Transition() {
	@Override
	public Architecture next(Architecture currentArchitecture, Architecture finalArchitecture) {
	    Architecture nextArchitecture = new Architecture(currentArchitecture);
	    for (String site : finalArchitecture.listSitesName()) {
		Set<Microservice> nextMicroservices = nextArchitecture.getSiteMicroservices(site);
		Iterator<Microservice> iterator = nextMicroservices.iterator();
		while (iterator.hasNext()) {
		    Microservice microservice = iterator.next();
		    if (SetUtil.noneMatch(finalArchitecture.getSiteMicroservices(site),
			    desiredMs -> microservice.isInstantiation(desiredMs))) {
			iterator.remove();
			logger.info("Removed microservice [{}]", microservice);
		    } else if (microservice.getVersion().equals(config.getUpdatingVersion())) {
			microservice.setVersion(VersionGenerator.random(SetUtil.collectVersions(nextMicroservices)));
			logger.info("Change microservice [{}] version from [{}] to [{}]", microservice.getName(),
				config.getUpdatingVersion(), microservice.getVersion());
		    }
		}
	    }
	    return nextArchitecture;
	}
    };

    /**
     * next architecture: adding new micro-services (in finalArchitecture, not
     * in currentArchitecture, identified by name)
     */
    public Transition addNewTransit = new Transition() {
	@Override
	public Architecture next(Architecture currentArchitecture, Architecture finalArchitecture) {
	    Architecture nextArchitecture = new Architecture(currentArchitecture);
	    for (String site : finalArchitecture.listSitesName()) {
		Set<Microservice> currentMicroservices = nextArchitecture.getSiteMicroservices(site);
		for (Microservice desiredMicroservice : finalArchitecture.getSiteMicroservices(site)) {
		    if (SetUtil.noneMatch(currentMicroservices,
			    ms -> ms.getName().equals(desiredMicroservice.getName()))) {
			Microservice newMicroservice = new Microservice(desiredMicroservice);
			newMicroservice.setGuid(null);
			Set<String> usedVersions = SetUtil.collectVersions(
				SetUtil.searchByName(currentMicroservices, desiredMicroservice.getName()));
			newMicroservice.setVersion(VersionGenerator.random(usedVersions));
			currentMicroservices.add(newMicroservice);
			logger.info("Added a new microservice: {} ", newMicroservice);
			continue;
		    }
		}
	    }
	    return nextArchitecture;
	}
    };

    /**
     * next architecture: remove old micro-services (in currentArchitecture, not
     * in finalArchitecture, identified by name)
     */
    public Transition removeOldTransit = new Transition() {
	@Override
	public Architecture next(Architecture currentArchitecture, Architecture finalArchitecture) {
	    Architecture nextArchitecture = new Architecture(currentArchitecture);
	    for (String site : finalArchitecture.listSitesName()) {
		Set<String> desiredMsName = SetUtil.collectNames(finalArchitecture.getSiteMicroservices(site));
		Set<Microservice> currentMicroservices = nextArchitecture.getSiteMicroservices(site);
		for (Microservice currentMicroservice : currentMicroservices) {
		    if (!desiredMsName.contains(currentMicroservice.getName())) {
			currentMicroservices.remove(currentMicroservice);
			logger.info("Removed an old microservice: {} ", currentMicroservice);
			continue;
		    }
		}
	    }
	    return nextArchitecture;
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
    public Transition directTransit(boolean tmpRoute) {
	return new Transition() {
	    @Override
	    public Architecture next(Architecture currentArchitecture, Architecture finalArchitecture) {
		Architecture nextArchitecture = new Architecture(finalArchitecture);
		for (String site : finalArchitecture.listSitesName()) {
		    for (Microservice nextMicroservice : nextArchitecture.getSiteMicroservices(site)) {
			Set<Microservice> currentMicroservices = SetUtil.searchByName(
				currentArchitecture.getSiteMicroservices(site), nextMicroservice.getName());
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
			    Microservice currentMicroservice = SetUtil.getOneMicroservice(
				    currentMicroservices, ms -> ms.getPath().equals(nextMicroservice.getPath())
					    && ms.getEnv().equals(nextMicroservice.getEnv()));
			    if (currentMicroservice == null) {
				currentMicroservice = currentMicroservices.iterator().next();
			    }
			    if (tmpRoute) {
				nextMicroservice.setRoutes(tmpRoute(site, nextMicroservice));
			    }
			    nextMicroservice.setGuid(currentMicroservice.getGuid());
			    nextMicroservice.setVersion(currentMicroservice.getVersion());
			    logger.info("{} detected as a updated microservice", nextMicroservice);
			}
		    }
		}
		return nextArchitecture;
	    }
	};
    }

    /**
     * getting next architecture by updating desired microservice route and
     * setting version
     */
    public Transition updateRouteTransit = new Transition() {
	// assume that it doesn't exist two microservices with same pkg and name
	@Override
	public Architecture next(Architecture currentArchitecture, Architecture finalArchitecture) {
	    Architecture nextArchitecture = new Architecture(currentArchitecture);
	    for (String site : finalArchitecture.listSitesName()) {
		for (Microservice desiredMicroservice : finalArchitecture.getSiteMicroservices(site)) {
		    Set<Microservice> nextMicroservices = SetUtil
			    .searchByName(nextArchitecture.getSiteMicroservices(site), desiredMicroservice.getName());
		    if (SetUtil.noneMatch(nextMicroservices, ms -> ms.isInstantiation(desiredMicroservice))) {
			Microservice nextMs = SetUtil.getOneMicroservice(nextMicroservices,
				ms -> ms.getVersion().equals(desiredVersion(desiredMicroservice))
					&& ms.getPath().equals(desiredMicroservice.getPath())
					&& ms.getEnv().equals(desiredMicroservice.getEnv())
					&& ms.getNbProcesses() == desiredMicroservice.getNbProcesses()
					&& ms.getServices().equals(desiredMicroservice.getServices())
					&& ms.getState().equals(desiredMicroservice.getState()));
			nextMs.setRoutes(desiredMicroservice.getRoutes());
			logger.info("Updated microservice [{}_{}] route to {} ", nextMs.getName(), nextMs.getVersion(),
				nextMs.getRoutes());
		    }
		}
	    }
	    return nextArchitecture;
	}
    };

    public Transition cleanAllTransit = new Transition() {
	@Override
	public Architecture next(Architecture currentArchitecture, Architecture finalArchitecture) {
	    Architecture nextArchitecture = new Architecture(currentArchitecture);
	    nextArchitecture.getSites().values().stream()
		    .forEach(s -> s.getMicroservices().clear());
	    return nextArchitecture;
	}
    };

    public Transition deployAllTransit = new Transition() {
	@Override
	public Architecture next(Architecture currentArchitecture, Architecture finalArchitecture) {
	    Architecture nextArchitecture = new Architecture(currentArchitecture);
	    for (String site : finalArchitecture.listSitesName()) {
		Set<Microservice> nextMicroservices = nextArchitecture.getSiteMicroservices(site);
		if (nextMicroservices.isEmpty()) {
		    nextMicroservices.addAll(finalArchitecture.getSiteMicroservices(site));
		}
	    }
	    return nextArchitecture;
	}
    };

    public Transition scaleHorizontalTransit = new Transition() {
	@Override
	public Architecture next(Architecture currentArchitecture, Architecture finalArchitecture) {
	    Architecture nextArchitecture = new Architecture(currentArchitecture);
	    for (String site : finalArchitecture.listSitesName()) {
		for (Microservice desiredMicroservice : finalArchitecture.getSiteMicroservices(site)) {
		    Microservice nextMicroservice = SetUtil.getUniqueMicroservice(
			    nextArchitecture.getSiteMicroservices(site), desiredMicroservice.getName(),
			    desiredVersion(desiredMicroservice));
		    if (nextMicroservice.getNbProcesses() != desiredMicroservice.getNbProcesses()) {
			nextMicroservice.setNbProcesses(desiredMicroservice.getNbProcesses());
			logger.info("Updated microservice [{}_{}] nbProcesses to {} ", nextMicroservice.getName(),
				nextMicroservice.getVersion(), nextMicroservice.getNbProcesses());
		    }
		}
	    }
	    return nextArchitecture;
	}
    };

    public Transition scaleIncrementalTransit = new Transition() {
	@Override
	public Architecture next(Architecture currentArchitecture, Architecture finalArchitecture) {
	    Architecture nextArchitecture = new Architecture(currentArchitecture);
	    for (String site : finalArchitecture.listSitesName()) {
		for (Microservice desiredMicroservice : finalArchitecture.getSiteMicroservices(site)) {
		    Microservice nextMicroservice = SetUtil.getUniqueMicroservice(
			    nextArchitecture.getSiteMicroservices(site), desiredMicroservice.getName(),
			    desiredVersion(desiredMicroservice));
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
	    return nextArchitecture;
	}
    };

    public Transition scaleVerticalTransit = new Transition() {
	@Override
	public Architecture next(Architecture currentArchitecture, Architecture finalArchitecture) {
	    Architecture nextArchitecture = new Architecture(currentArchitecture);
	    for (String site : finalArchitecture.listSitesName()) {
		for (Microservice desiredMicroservice : finalArchitecture.getSiteMicroservices(site)) {
		    Microservice nextMicroservice = SetUtil.getUniqueMicroservice(
			    nextArchitecture.getSiteMicroservices(site), desiredMicroservice.getName(),
			    desiredVersion(desiredMicroservice));
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
	    return nextArchitecture;
	}
    };

    public Transition cleanNotUpdatableMicroserviceTransit = new Transition() {
	@Override
	public Architecture next(Architecture currentArchitecture, Architecture finalArchitecture) {
	    Architecture nextArchitecture = new Architecture(currentArchitecture);
	    for (String site : finalArchitecture.listSitesName()) {
		// based on the assumption that path is set to "" in case of
		// null, during getCurrentArchitecture
		Set<Microservice> nextMicroservices = nextArchitecture.getSiteMicroservices(site);
		Set<Microservice> nonUpdatableMicroservice = SetUtil.search(nextMicroservices,
			ms -> "".equals(ms.getPath()) || ms.getState().equals(MicroserviceState.CREATED));
		if (!nonUpdatableMicroservice.isEmpty()) {
		    logger.info("clean up not updatable microservices: {}", nonUpdatableMicroservice);
		    nextMicroservices.removeAll(nonUpdatableMicroservice);
		}
	    }
	    return nextArchitecture;
	}
    };

    public String desiredVersion(Microservice desiredMicroservice) {
	return desiredMicroservice.getVersion() == null ? config.getUpdatingVersion()
		: desiredMicroservice.getVersion();
    }

    public Set<String> tmpRoute(String site, Microservice microservice) {
	return Collections.singleton(config.getSiteConfig(site).getTmpRoute(microservice.getName()));
    }
}

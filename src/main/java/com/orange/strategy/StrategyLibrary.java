package com.orange.strategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.model.StrategyConfig;
import com.orange.model.architecture.Architecture;
import com.orange.model.architecture.Microservice;
import com.orange.model.architecture.MicroserviceState;
import com.orange.util.SetUtil;

public class StrategyLibrary {
    private Logger logger;
    private StrategyConfig config;

    public StrategyLibrary(StrategyConfig config) {
	this.logger = LoggerFactory.getLogger(StrategyLibrary.class);
	this.config = config;
    }

    /**
     * next architecture: adding new micro-services (in finalArchitecture, not in
     * currentArchitecture, identified by name)
     */
    public Transition addNewTransit = new Transition() {
	@Override
	public Architecture next(Architecture currentArchitecture, Architecture finalArchitecture) {
	    Architecture nextArchitecture = new Architecture(currentArchitecture);
	    for (String site : finalArchitecture.listSitesName()) {
		Set<Microservice> currentMicroservices = nextArchitecture.getSiteMicroservices(site);
		for (Microservice desiredMicroservice : finalArchitecture.getSiteMicroservices(site)) {
		    if (SetUtil.noneMatch(currentMicroservices,
			    ms -> ms.get("name").equals(desiredMicroservice.get("name")))) {
			Microservice newMicroservice = new Microservice(desiredMicroservice);
			newMicroservice.set("guid", null);
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
		    }
		}
	    }
	    return nextArchitecture;
	}
    };

    /**
     * next architecture: remove old micro-services (in currentArchitecture, not in
     * finalArchitecture, identified by name)
     */
    public Transition removeOldTransit = new Transition() {
	@Override
	public Architecture next(Architecture currentArchitecture, Architecture finalArchitecture) {
	    Architecture nextArchitecture = new Architecture(currentArchitecture);
	    for (String site : finalArchitecture.listSitesName()) {
		Set<String> desiredMsName = SetUtil.collectNames(finalArchitecture.getSiteMicroservices(site));
		Set<Microservice> currentMicroservices = nextArchitecture.getSiteMicroservices(site);
		for (Microservice currentMicroservice : currentMicroservices) {
		    if (!desiredMsName.contains(currentMicroservice.get("name"))) {
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
     * next architecture: direct in-place update current most similar microservice
     * to desired
     * 
     * @param tmpRoute
     *            whether map new or path/env updated microservices to a temporary
     *            route
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
				currentArchitecture.getSiteMicroservices(site), (String) nextMicroservice.get("name"));
			if (currentMicroservices.size() == 0) {
			    // Add non-exist microservice
			    nextMicroservice.set("guid", null);
			    if (tmpRoute) {
				nextMicroservice.set("routes", tmpRoute(site, nextMicroservice));
			    }
			    logger.info("{} detected as a new microservice.", nextMicroservice);
			} else {
			    // update from the most similar microservice (i.e. path and env equals)
			    Microservice currentMicroservice = SetUtil.getOneMicroservice(currentMicroservices,
				    ms -> ms.get("path").equals(nextMicroservice.get("path"))
					    && ms.get("env").equals(nextMicroservice.get("env")));
			    if (currentMicroservice == null) {
				// update from any version of the microservice when not exist path and env eq ms
				currentMicroservice = currentMicroservices.iterator().next();
				if (tmpRoute) {
				    nextMicroservice.set("routes", tmpRoute(site, nextMicroservice));
				}
			    }
			    nextMicroservice.set("guid", currentMicroservice.get("guid"));
			    if (!currentMicroservice.isInstantiation(nextMicroservice)) {
				logger.info("{} detected as a updated microservice", nextMicroservice);
			    }
			}
		    }
		}
		// re-copy nextArchitecture to avoid Set contains problem, as ms is modified
		return new Architecture(nextArchitecture);
	    }
	};
    }

    /**
     * update route to the desired in the condition that all the other properties
     * except updateAfterKeys have been updated
     * 
     * @param updateAfterwardKeys
     * @return
     */
    public Transition updateRouteTransit(List<String> updateAfterwardKeys) {
	return new Transition() {
	    @Override
	    public Architecture next(Architecture currentArchitecture, Architecture finalArchitecture) {
		Architecture nextArchitecture = new Architecture(currentArchitecture);
		List<String> notUpdatedKeys = new ArrayList<>(updateAfterwardKeys);
		notUpdatedKeys.add("routes");
		for (String site : finalArchitecture.listSitesName()) {
		    for (Microservice desiredMs : finalArchitecture.getSiteMicroservices(site)) {
			Set<Microservice> nextMss = nextArchitecture.getSiteMicroservices(site);
			Microservice nextMs = SetUtil.getUniqueMicroservice(nextMss, (String) desiredMs.get("name"),
				(String) desiredMs.get("version"));
			if (nextMs != null && nextMs.eqAttrExcept(notUpdatedKeys, desiredMs)
				&& !nextMs.eqAttr("routes", desiredMs)) {
			    nextMs.copyAttr("routes", desiredMs);
			    logger.info("Updated microservice [{}_{}] route to {} ", nextMs.get("name"),
				    nextMs.get("version"), nextMs.get("routes"));
			}
		    }
		}
		return nextArchitecture;
	    }
	};
    }

    public Transition cleanAllTransit = new Transition() {
	@Override
	public Architecture next(Architecture currentArchitecture, Architecture finalArchitecture) {
	    Architecture nextArchitecture = new Architecture(currentArchitecture);
	    nextArchitecture.getSites().values().stream().forEach(s -> s.getMicroservices().clear());
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
		    Microservice nextMs = SetUtil.getUniqueMicroservice(nextArchitecture.getSiteMicroservices(site),
			    (String) desiredMicroservice.get("name"), (String) desiredMicroservice.get("version"));
		    if (nextMs != null && nextMs.get("nbProcesses") != desiredMicroservice.get("nbProcesses")) {
			nextMs.set("nbProcesses", desiredMicroservice.get("nbProcesses"));
			logger.info("Updated microservice [{}_{}] nbProcesses to {} ", nextMs.get("name"),
				nextMs.get("version"), nextMs.get("nbProcesses"));
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
		for (Microservice desiredMs : finalArchitecture.getSiteMicroservices(site)) {
		    Microservice nextMs = SetUtil.getUniqueMicroservice(nextArchitecture.getSiteMicroservices(site),
			    (String) desiredMs.get("name"), (String) desiredMs.get("version"));
		    if (nextMs != null && (int) nextMs.get("nbProcesses") <= (int) desiredMs.get("nbProcesses")) {
			int nextNbr = (int) nextMs.get("nbProcesses") + config.getCanaryIncrease();
			nextNbr = nextNbr > (int) desiredMs.get("nbProcesses") ? (int) desiredMs.get("nbProcesses")
				: nextNbr;
			nextMs.set("nbProcesses", nextNbr);
			logger.info("Updated microservice [{}_{}] nbProcesses to {} ", nextMs.get("name"),
				nextMs.get("version"), nextNbr);
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
		for (Microservice desiredMs : finalArchitecture.getSiteMicroservices(site)) {
		    Microservice nextMs = SetUtil.getUniqueMicroservice(nextArchitecture.getSiteMicroservices(site),
			    (String) desiredMs.get("name"), (String) desiredMs.get("version"));
		    if (nextMs != null) {
			if (!nextMs.get("memory").equals(desiredMs.get("memory"))) {
			    nextMs.set("memory", desiredMs.get("memory"));
			    logger.info("Updated microservice [{}_{}] memory to {} ", nextMs.get("name"),
				    nextMs.get("version"), nextMs.get("memory"));
			}
			if (!nextMs.get("disk").equals(desiredMs.get("disk"))) {
			    nextMs.set("disk", desiredMs.get("disk"));
			    logger.info("Updated microservice [{}_{}] disk to {} ", nextMs.get("name"),
				    nextMs.get("version"), nextMs.get("disk"));
			}
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
			ms -> "".equals(ms.get("path")) || ms.get("state").equals(MicroserviceState.CREATED));
		if (!nonUpdatableMicroservice.isEmpty()) {
		    logger.info("clean up not updatable microservices: {}", nonUpdatableMicroservice);
		    nextMicroservices.removeAll(nonUpdatableMicroservice);
		}
	    }
	    return nextArchitecture;
	}
    };

    public Set<String> tmpRoute(String site, Microservice microservice) {
	return Collections.singleton(config.getSiteConfig(site).getTmpRoute((String) microservice.get("name")));
    }
}

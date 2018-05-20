package com.orange.strategy;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
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
import com.orange.util.VersionGenerator;

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
			Set<String> usedVersions = SetUtil.collectVersions(
				SetUtil.searchByName(currentMicroservices, (String) desiredMicroservice.get("name")));
			newMicroservice.set("version", VersionGenerator.random(usedVersions));
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
		    } else if (microservice.get("version").equals(config.getUpdatingVersion())) {
			microservice.set("version",
				VersionGenerator.random(SetUtil.collectVersions(nextMicroservices)));
			logger.info("Change microservice [{}] version from [{}] to [{}]", microservice.get("name"),
				config.getUpdatingVersion(), microservice.get("version"));
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
			    nextMicroservice.set("version", VersionGenerator.random(new HashSet<>()));
			    if (tmpRoute) {
				nextMicroservice.set("routes", tmpRoute(site, nextMicroservice));
			    }
			    logger.info("{} detected as a new microservice.", nextMicroservice);
			} else {
			    // update from most similar microservice (i.e.
			    // prefer path and env equals if exist)
			    Microservice currentMicroservice = SetUtil.getOneMicroservice(currentMicroservices,
				    ms -> ms.get("path").equals(nextMicroservice.get("path"))
					    && ms.get("env").equals(nextMicroservice.get("env")));
			    if (currentMicroservice == null) {
				currentMicroservice = currentMicroservices.iterator().next();
			    }
			    if (tmpRoute) {
				nextMicroservice.set("routes", tmpRoute(site, nextMicroservice));
			    }
			    nextMicroservice.set("guid", currentMicroservice.get("guid"));
			    nextMicroservice.set("version", currentMicroservice.get("version"));
			    logger.info("{} detected as a updated microservice", nextMicroservice);
			}
		    }
		}
		return nextArchitecture;
	    }
	};
    }

    public Transition updateRouteTransit(List<String> updatingKeys) {
	return new Transition() {
	    @Override
	    public Architecture next(Architecture currentArchitecture, Architecture finalArchitecture) {
		Architecture nextArchitecture = new Architecture(currentArchitecture);
		for (String site : finalArchitecture.listSitesName()) {
		    for (Microservice desiredMs : finalArchitecture.getSiteMicroservices(site)) {
			Set<Microservice> nextMss = nextArchitecture.getSiteMicroservices(site);
			if (SetUtil.noneMatch(nextMss, ms -> ms.eqAttrExcept(updatingKeys, desiredMs)
				&& ms.get("version").equals(desiredVersion(desiredMs)))) {
			    Microservice nextMs = SetUtil.getUniqueMicroservice(nextMss, (String) desiredMs.get("name"),
				    desiredVersion(desiredMs));
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

    /**
     * getting next architecture by updating desired microservice route and setting
     * version
     */
    public Transition updateRouteAtLastTransit = updateRouteTransit(Arrays.asList("guid", "version"));

    /**
     * next architecture: update desired microservice route
     */
    public Transition updateRouteBeforeNbProcTransit = updateRouteTransit(
	    Arrays.asList("guid", "version", "nbProcesses"));

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
		    Microservice nextMicroservice = SetUtil.getUniqueMicroservice(
			    nextArchitecture.getSiteMicroservices(site), (String) desiredMicroservice.get("name"),
			    desiredVersion(desiredMicroservice));
		    if (nextMicroservice.get("nbProcesses") != desiredMicroservice.get("nbProcesses")) {
			nextMicroservice.set("nbProcesses", desiredMicroservice.get("nbProcesses"));
			logger.info("Updated microservice [{}_{}] nbProcesses to {} ", nextMicroservice.get("name"),
				nextMicroservice.get("version"), nextMicroservice.get("nbProcesses"));
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
			    nextArchitecture.getSiteMicroservices(site), (String) desiredMicroservice.get("name"),
			    desiredVersion(desiredMicroservice));
		    if ((int) nextMicroservice.get("nbProcesses") <= (int) desiredMicroservice.get("nbProcesses")) {
			int nextNbr = (int) nextMicroservice.get("nbProcesses") + config.getCanaryIncrease();
			nextNbr = nextNbr > (int) desiredMicroservice.get("nbProcesses")
				? (int) desiredMicroservice.get("nbProcesses")
				: nextNbr;
			nextMicroservice.set("nbProcesses", nextNbr);
			logger.info("Updated microservice [{}_{}] nbProcesses to {} ", nextMicroservice.get("name"),
				nextMicroservice.get("version"), nextNbr);
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
			    nextArchitecture.getSiteMicroservices(site), (String) desiredMicroservice.get("name"),
			    desiredVersion(desiredMicroservice));
		    if (nextMicroservice.get("memory").equals(desiredMicroservice.get("memory"))) {
			nextMicroservice.set("memory", desiredMicroservice.get("memory"));
			logger.info("Updated microservice [{}_{}] memory to {} ", nextMicroservice.get("name"),
				nextMicroservice.get("version"), nextMicroservice.get("memory"));
		    }
		    if (nextMicroservice.get("disk").equals(desiredMicroservice.get("disk"))) {
			nextMicroservice.set("disk", desiredMicroservice.get("disk"));
			logger.info("Updated microservice [{}_{}] disk to {} ", nextMicroservice.get("name"),
				nextMicroservice.get("version"), nextMicroservice.get("disk"));
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

    public String desiredVersion(Microservice desiredMicroservice) {
	return desiredMicroservice.get("version") == null ? config.getUpdatingVersion()
		: (String) desiredMicroservice.get("version");
    }

    public Set<String> tmpRoute(String site, Microservice microservice) {
	return Collections.singleton(config.getSiteConfig(site).getTmpRoute((String) microservice.get("name")));
    }
}

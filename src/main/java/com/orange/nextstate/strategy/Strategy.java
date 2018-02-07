package com.orange.nextstate.strategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.model.StrategyConfig;
import com.orange.model.state.Architecture;
import com.orange.model.state.ArchitectureMicroservice;

public abstract class Strategy {
    private static final Logger logger = LoggerFactory.getLogger(Strategy.class);

    protected List<Transition> transitions = new ArrayList<Transition>();

    public abstract boolean valid(Architecture currentState, Architecture finalState);
    // public abstract List<Transition> transitions();

    protected StrategyConfig config;
    protected StrategyLibrary library;

    public Strategy(StrategyConfig config) {
	this.config = config;
	this.library = new StrategyLibrary(this);
    }

    public List<Transition> getTransitions() {
	return transitions;
    }

    /**
     * for the strategy which is configured to be updated all sites in parallel
     * (i.e.config.parallelAllSites is true), calculate next mid architecture to
     * achieve the final architecture, based on current architecture, update
     * strategy and deployment configurations.
     * 
     * @param currentArchitecture
     * @param finalArchitecture
     * @return
     */
    public Architecture nextArchitecture(final Architecture currentArchitecture, final Architecture finalArchitecture) {
	if (!valid(currentArchitecture, finalArchitecture)) {
	    throw new IllegalStateException("Strategy disallowed situation");
	}
	for (Transition transition : transitions) {
	    Architecture next = transition.next(currentArchitecture, finalArchitecture);
	    if (!next.equals(currentArchitecture)) {
		return next;
	    }
	}
	return finalArchitecture;
    }

    /**
     * depending on the config.sitesOrder, calculate next mid architecture to
     * achieve the final architecture.
     * 
     * @param currentArchitecture
     * @param finalArchitecture
     * @return
     */
    public Architecture nextArchitectureSitesOrdered(final Architecture currentArchitecture,
	    final Architecture finalArchitecture) {
	config.validSitesOrder(finalArchitecture.listSitesName());
	Architecture nextStates = new Architecture(currentArchitecture);
	for (Set<String> sites : config.getSitesOrder()) {
	    Architecture currentSubArchitecture = currentArchitecture.getSubArchitecture(sites);
	    Architecture finalSubArchitecture = finalArchitecture.getSubArchitecture(sites);
	    if (isInstantiation(currentSubArchitecture, finalSubArchitecture)) {
		logger.info("Sites {} are already the instantiation of the final state.", sites);
		continue;
	    } else {
		Architecture updatedSitesArchitecture = nextArchitecture(currentSubArchitecture, finalSubArchitecture);
		logger.info("Get next state {} for the sites {}.", updatedSitesArchitecture, sites);
		nextStates.mergeArchitecture(updatedSitesArchitecture);
		return nextStates;
	    }
	}
	logger.error(
		"Abnormal state in calcNextStates: not found sites which is not already the instantiation of the final state.");
	return null;
    }

    /**
     * return whether currentArchitecture is an instantiated architecture of
     * finalArchitecture.
     * 
     * @param currentArchitecture
     * @param finalArchitecture
     * @return
     */
    public boolean isInstantiation(Architecture currentArchitecture, Architecture finalArchitecture) {
	if (finalArchitecture == null) {
	    return false;
	}
	if (!currentArchitecture.getSites().equals(finalArchitecture.getSites())) {
	    return false;
	}
	for (String site : currentArchitecture.listSitesName()) {
	    Set<ArchitectureMicroservice> desiredMicroservices = finalArchitecture.getArchitectureMicroservices(site);
	    Set<ArchitectureMicroservice> microservices = currentArchitecture.getArchitectureMicroservices(site);
	    if (microservices.size() != desiredMicroservices.size()) {
		return false;
	    }
	    for (ArchitectureMicroservice desiredMicroservice : desiredMicroservices) {
		if (microservices.stream().noneMatch(ms -> isInstantiation(ms, desiredMicroservice))) {
		    return false;
		}
	    }
	}
	return true;
    }

    /**
     * return whether currentMicroservice is an instantiated microservice of
     * desiredMicroservice.
     * 
     * @param currentState
     * @param desiredState
     * @return
     */
    protected boolean isInstantiation(ArchitectureMicroservice currentMicroservice,
	    ArchitectureMicroservice desiredMicroservice) {
	if (desiredMicroservice == null) {
	    return false;
	}
	if (desiredMicroservice.getGuid() != null
		&& !desiredMicroservice.getGuid().equals(currentMicroservice.getGuid())) {
	    return false;
	}
	if (desiredMicroservice.getVersion() != null
		&& !desiredMicroservice.getVersion().equals(currentMicroservice.getVersion())) {
	    return false;
	}
	if (!currentMicroservice.getName().equals(desiredMicroservice.getName())
		|| !currentMicroservice.getPath().equals(desiredMicroservice.getPath())
		|| !currentMicroservice.getState().equals(desiredMicroservice.getState())
		|| currentMicroservice.getNbProcesses() != desiredMicroservice.getNbProcesses()
		|| !currentMicroservice.getEnv().equals(desiredMicroservice.getEnv())
		|| !currentMicroservice.getRoutes().equals(desiredMicroservice.getRoutes())
		|| !currentMicroservice.getMemory().equals(desiredMicroservice.getMemory())
		|| !currentMicroservice.getDisk().equals(desiredMicroservice.getDisk())) {
	    return false;
	}
	return true;
    }

}

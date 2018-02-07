package com.orange.nextstate.strategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.model.StrategyConfig;
import com.orange.model.state.Architecture;

public abstract class Strategy {
    private static final Logger logger = LoggerFactory.getLogger(Strategy.class);

    protected List<Transition> transitions = new ArrayList<Transition>();

    public abstract boolean valid(Architecture currentState, Architecture finalState);
    // public abstract List<Transition> transitions();

    protected StrategyConfig config;
    protected StrategyLibrary library;

    public Strategy(StrategyConfig config) {
	this.config = config;
	this.library = new StrategyLibrary(config);
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
	    // get the subset of the architecture with the sites to be updated.
	    Architecture currentSubArchitecture = currentArchitecture.getSubArchitecture(sites);
	    Architecture finalSubArchitecture = finalArchitecture.getSubArchitecture(sites);
	    if (currentSubArchitecture.isInstantiation(finalSubArchitecture)) {
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

}

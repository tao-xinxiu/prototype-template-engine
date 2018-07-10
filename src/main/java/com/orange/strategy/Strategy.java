package com.orange.strategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.model.StrategyConfig;
import com.orange.model.architecture.Architecture;

public abstract class Strategy {
    private static final Logger logger = LoggerFactory.getLogger(Strategy.class);

    protected List<Transition> transitions = new ArrayList<Transition>();

    // decide the cases that the strategy can deal with. can also be used to
    // enfore some deployment displine.
    public abstract boolean valid(Architecture currentArchitecture, Architecture finalArchitecture);
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
		logger.info("using the transition: " + transition.getClass().getName());
		return next;
	    }
	}
	logger.info("arrived the final architecture.");
	return null;
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
	Architecture nextArchitecture = new Architecture(currentArchitecture);
	for (Set<String> sites : config.getSitesOrder()) {
	    // get the subset of the architecture with the sites to be updated.
	    Architecture currentSubArchitecture = currentArchitecture.getSubArchitecture(sites);
	    Architecture finalSubArchitecture = finalArchitecture.getSubArchitecture(sites);
	    if (currentSubArchitecture.isInstantiation(finalSubArchitecture)) {
		logger.info("Sites {} are already the instantiation of the final architecture.", sites);
		continue;
	    } else {
		Architecture updatedSitesArchitecture = nextArchitecture(currentSubArchitecture, finalSubArchitecture);
		logger.info("Get next architecture {} for the sites {}.", updatedSitesArchitecture, sites);
		nextArchitecture.mergeArchitecture(updatedSitesArchitecture);
		return nextArchitecture;
	    }
	}
	return null;
    }

}

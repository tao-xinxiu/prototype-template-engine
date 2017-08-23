package com.orange.midstate;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.midstate.strategy.Strategy;
import com.orange.midstate.strategy.Transit;
import com.orange.model.StrategyConfig;
import com.orange.model.state.Overview;

public class MidStateCalculator {
    private static final Logger logger = LoggerFactory.getLogger(MidStateCalculator.class);

    private String strategyClass;
    private StrategyConfig strategyConfig;

    public MidStateCalculator(String strategyClass, StrategyConfig strategyConfig) {
	this.strategyClass = strategyClass;
	this.strategyConfig = strategyConfig;
    }

    /**
     * calculate next mid state to achieve the final state, based on current
     * state and different deployment configurations and update strategies.
     * 
     * @param currentState
     * @param finalState
     * @return
     */
    public Overview calcNextStates(final Overview currentState, final Overview finalState) {
	// return null when arrived final state
	if (currentState.isInstantiation(finalState)) {
	    return null;
	}
	if (!strategyConfig.isParallelAllSites()) {
	    validSitesOrder(finalState.listSitesName());
	    Overview nextStates = new Overview(currentState);
	    for (Set<String> sites : strategyConfig.getSitesOrder()) {
		Overview currentSubOverview = currentState.getSubOverview(sites);
		Overview finalSubOverview = finalState.getSubOverview(sites);
		if (currentSubOverview.isInstantiation(finalSubOverview)) {
		    logger.info("Sites {} are already the instantiation of the final state.", sites);
		    continue;
		} else {
		    Overview updatedSitesOverview = strategyNextStates(currentSubOverview, finalSubOverview);
		    logger.info("Get next state {} for the sites {}.", updatedSitesOverview, sites);
		    nextStates.mergeOverview(updatedSitesOverview);
		    return nextStates;
		}
	    }
	    logger.error(
		    "Abnormal state in calcNextStates: not found sites which is not already the instantiation of the final state.");
	    return null;
	} else {
	    return strategyNextStates(currentState, finalState);
	}
    }

    private Overview strategyNextStates(final Overview currentState, final Overview finalState) {
	try {
	    Strategy strategy = (Strategy) Class.forName(strategyClass).getConstructor(StrategyConfig.class)
		    .newInstance(strategyConfig);
	    if (!strategy.valid(currentState, finalState)) {
		throw new IllegalStateException("Strategy disallowed situation");
	    }
	    for (Transit transit : strategy.transits()) {
		Overview next = transit.next(currentState, finalState);
		if (!next.equals(currentState)) {
		    return next;
		}
	    }
	    return finalState;
	} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
		| NoSuchMethodException | SecurityException | ClassNotFoundException e) {
	    throw new IllegalStateException(String.format("Exception [%s] in Strategy [%s] instantiation.",
		    e.getClass().getName(), strategyClass), e);
	}
    }

    private void validSitesOrder(Set<String> completeSites) {
	List<Set<String>> sitesOrder = strategyConfig.getSitesOrder();
	if (sitesOrder.isEmpty()) {
	    throw new IllegalStateException(
		    "strategyConfig.sitesOrder is not specified for a non-parallel update sites strategy.");
	}
	List<String> sitesInOrder = sitesOrder.stream().flatMap(s -> s.stream()).collect(Collectors.toList());
	if (completeSites.size() != sitesInOrder.size()) {
	    throw new IllegalStateException(
		    "Number of sites in strategyConfig.sitesOrder is not equal to the number of sites specified in the finalState.");
	}
	if (!completeSites.equals(new HashSet<>(sitesInOrder))) {
	    throw new IllegalStateException(
		    "sites in strategyConfig.sitesOrder is not equal to the sites specified in the finalState");
	}
    }
}

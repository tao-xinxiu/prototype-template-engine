package com.orange.nextstate;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.model.StrategyConfig;
import com.orange.model.state.Architecture;
import com.orange.nextstate.strategy.Strategy;
import com.orange.nextstate.strategy.Transit;

public class NextStateCalculator {
    private static final Logger logger = LoggerFactory.getLogger(NextStateCalculator.class);

    private String strategyClass;
    private StrategyConfig strategyConfig;

    public NextStateCalculator(String strategyClass, StrategyConfig strategyConfig) {
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
    public Architecture calcNextStates(final Architecture currentState, final Architecture finalState) {
	// return null when arrived final state
	if (currentState.isInstantiation(finalState)) {
	    return null;
	}
	if (!strategyConfig.isParallelAllSites()) {
	    validSitesOrder(finalState.listSitesName());
	    Architecture nextStates = new Architecture(currentState);
	    for (Set<String> sites : strategyConfig.getSitesOrder()) {
		Architecture currentSubArchitecture = currentState.getSubArchitecture(sites);
		Architecture finalSubArchitecture = finalState.getSubArchitecture(sites);
		if (currentSubArchitecture.isInstantiation(finalSubArchitecture)) {
		    logger.info("Sites {} are already the instantiation of the final state.", sites);
		    continue;
		} else {
		    Architecture updatedSitesArchitecture = strategyNextStates(currentSubArchitecture, finalSubArchitecture);
		    logger.info("Get next state {} for the sites {}.", updatedSitesArchitecture, sites);
		    nextStates.mergeArchitecture(updatedSitesArchitecture);
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

    private Architecture strategyNextStates(final Architecture currentState, final Architecture finalState) {
	try {
	    Strategy strategy = (Strategy) Class.forName(strategyClass).getConstructor(StrategyConfig.class)
		    .newInstance(strategyConfig);
	    if (!strategy.valid(currentState, finalState)) {
		throw new IllegalStateException("Strategy disallowed situation");
	    }
	    for (Transit transit : strategy.transits()) {
		Architecture next = transit.next(currentState, finalState);
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

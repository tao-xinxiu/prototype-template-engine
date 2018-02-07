package com.orange.nextstate;

import java.lang.reflect.InvocationTargetException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.model.StrategyConfig;
import com.orange.model.state.Architecture;
import com.orange.nextstate.strategy.Strategy;

public class NextStateCalculator {
    private static final Logger logger = LoggerFactory.getLogger(NextStateCalculator.class);

    private StrategyConfig strategyConfig;
    private Strategy strategy;

    public NextStateCalculator(String strategyClass, StrategyConfig strategyConfig) {
	this.strategyConfig = strategyConfig;
	try {
	    this.strategy = (Strategy) Class.forName(strategyClass).getConstructor(StrategyConfig.class)
		    .newInstance(strategyConfig);
	} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
		| NoSuchMethodException | SecurityException | ClassNotFoundException e) {
	    throw new IllegalStateException(String.format("Exception [%s] in Strategy [%s] instantiation.",
		    e.getClass().getName(), strategyClass), e);
	}
    }

    public boolean isInstantiation(final Architecture currentArchitecture, final Architecture finalArchitecture) {
	return strategy.isInstantiation(currentArchitecture, finalArchitecture);
    }

    public Architecture nextArchitecture(final Architecture currentArchitecture, final Architecture finalArchitecture) {
	// return null when arrived final state
	if (isInstantiation(currentArchitecture, finalArchitecture)) {
	    return null;
	}
	if (strategyConfig.isParallelAllSites()) {
	    return strategy.nextArchitecture(currentArchitecture, finalArchitecture);
	} else {
	    return strategy.nextArchitectureSitesOrdered(currentArchitecture, finalArchitecture);
	}
    }
}

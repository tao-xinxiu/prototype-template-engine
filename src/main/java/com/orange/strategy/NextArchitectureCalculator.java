package com.orange.strategy;

import java.lang.reflect.InvocationTargetException;

import com.orange.model.StrategyConfig;
import com.orange.model.architecture.Architecture;

public class NextArchitectureCalculator {
    private StrategyConfig strategyConfig;
    private Strategy strategy;

    public NextArchitectureCalculator(String strategyClass, StrategyConfig strategyConfig) {
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

    public Architecture nextArchitecture(final Architecture currentArchitecture, final Architecture finalArchitecture) {
	if (strategyConfig.isParallelAllSites()) {
	    return strategy.nextArchitecture(currentArchitecture, finalArchitecture);
	} else {
	    return strategy.nextArchitectureSitesOrdered(currentArchitecture, finalArchitecture);
	}
    }
}

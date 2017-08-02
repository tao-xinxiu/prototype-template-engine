package com.orange.midstate;

import java.lang.reflect.InvocationTargetException;

import com.orange.midstate.strategy.Strategy;
import com.orange.midstate.strategy.TransitPoint;
import com.orange.model.StrategyConfig;
import com.orange.model.state.Overview;

public class MidStateCalculator {
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
    public Overview calcMidStates(final Overview currentState, final Overview finalState) {
	// return null when arrived final state
	if (currentState.isInstantiation(finalState)) {
	    return null;
	}
	try {
	    Strategy strategy = (Strategy) Class.forName(strategyClass).getConstructor(StrategyConfig.class)
		    .newInstance(strategyConfig);
	    if (!strategy.valid(currentState, finalState)) {
		throw new IllegalStateException("Strategy disallowed situation");
	    }
	    for (TransitPoint transitPoint : strategy.transitPoints()) {
		if (transitPoint.condition(currentState, finalState)) {
		    return transitPoint.next(currentState, finalState);
		}
	    }
	    return finalState;
	} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
		| NoSuchMethodException | SecurityException | ClassNotFoundException e) {
	    throw new IllegalStateException(String.format("Exception [%s] in Strategy [%s] instantiation.",
		    e.getClass().getName(), strategyClass), e);
	}
    }
}

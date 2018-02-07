package com.orange.nextstate.strategy;

import java.util.Arrays;

import com.orange.model.StrategyConfig;

public class InplaceTestStrategy extends InplaceStrategy {

    public InplaceTestStrategy(StrategyConfig config) {
	super(config);
	transitions = Arrays.asList(library.directTransit(true), library.updateRouteTransit);
    }

}

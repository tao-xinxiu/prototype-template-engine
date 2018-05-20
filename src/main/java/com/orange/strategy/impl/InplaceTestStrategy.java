package com.orange.strategy.impl;

import java.util.Arrays;

import com.orange.model.StrategyConfig;

public class InplaceTestStrategy extends StraightStrategy {

    public InplaceTestStrategy(StrategyConfig config) {
	super(config);
	transitions = Arrays.asList(library.directTransit(true), library.updateRouteAtLastTransit);
    }

}

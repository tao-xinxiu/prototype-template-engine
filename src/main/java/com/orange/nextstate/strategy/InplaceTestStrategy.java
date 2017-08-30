package com.orange.nextstate.strategy;

import java.util.Arrays;
import java.util.List;

import com.orange.model.StrategyConfig;

public class InplaceTestStrategy extends InplaceStrategy {

    public InplaceTestStrategy(StrategyConfig config) {
	super(config);
    }

    @Override
    public List<Transit> transits() {
	return Arrays.asList(library.directTransit(true), library.updateRouteTransit);
    }

}

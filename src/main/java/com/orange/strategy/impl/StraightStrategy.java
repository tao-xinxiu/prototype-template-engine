package com.orange.strategy.impl;

import java.util.Arrays;

import com.orange.model.StrategyConfig;
import com.orange.model.architecture.Architecture;
import com.orange.strategy.Strategy;

public class StraightStrategy extends Strategy {

    public StraightStrategy(StrategyConfig config) {
	super(config);
	transitions = Arrays.asList(library.directTransit(false));
    }

    @Override
    public boolean valid(Architecture currentArchitecture, Architecture finalArchitecture) {
	return true;
    }
}

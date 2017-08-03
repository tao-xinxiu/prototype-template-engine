package com.orange.midstate.strategy;

import java.util.List;

import com.orange.model.StrategyConfig;
import com.orange.model.state.Overview;

public abstract class Strategy {
    protected StrategyConfig config;
    protected StrategyLibrary library;

    public Strategy(StrategyConfig config) {
	this.config = config;
	this.library = new StrategyLibrary(config);
    }

    public abstract boolean valid(Overview currentState, Overview finalState);

    public abstract List<TransitPoint> transitPoints();

}

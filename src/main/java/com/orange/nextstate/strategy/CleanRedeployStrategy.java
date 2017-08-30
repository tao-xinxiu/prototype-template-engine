package com.orange.nextstate.strategy;

import java.util.Arrays;
import java.util.List;

import com.orange.model.StrategyConfig;
import com.orange.model.state.Overview;

public class CleanRedeployStrategy extends Strategy {
    public CleanRedeployStrategy(StrategyConfig config) {
	super(config);
    }

    @Override
    public boolean valid(Overview currentState, Overview finalState) {
	return true;
    }

    @Override
    public List<Transit> transits() {
	return Arrays.asList(library.cleanAllTransit, library.deployAllTransit);
    }

}

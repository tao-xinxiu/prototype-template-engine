package com.orange.nextstate.strategy;

import java.util.Arrays;

import com.orange.model.StrategyConfig;
import com.orange.model.state.Architecture;

public class CleanRedeployStrategy extends Strategy {
    public CleanRedeployStrategy(StrategyConfig config) {
	super(config);
	transitions = Arrays.asList(library.cleanAllTransit, library.deployAllTransit);
    }

    @Override
    public boolean valid(Architecture currentState, Architecture finalState) {
	return true;
    }

}

package com.orange.nextstate.strategy;

import java.util.Arrays;
import java.util.Set;

import com.orange.model.StrategyConfig;
import com.orange.model.state.Architecture;
import com.orange.util.SetUtil;

public class RemoveAddStrategy extends Strategy {
    public RemoveAddStrategy(StrategyConfig config) {
	super(config);
	transitions = Arrays.asList(library.removeOldTransit, library.addNewTransit);
    }

    /**
     * This strategy only deal with additions and removals of microservice. i.e.
     * It should not exist microservice (identify by name) which is in both
     * currentState and finalState
     */
    @Override
    public boolean valid(Architecture currentState, Architecture finalState) {
	for (String site : finalState.listSitesName()) {
	    Set<String> currentMsName = SetUtil.collectNames(currentState.getArchitectureMicroservices(site));
	    Set<String> finalMsName = SetUtil.collectNames(finalState.getArchitectureMicroservices(site));
	    if (currentMsName.stream().anyMatch(finalMsName::contains)) {
		return false;
	    }
	}
	return true;
    }

}

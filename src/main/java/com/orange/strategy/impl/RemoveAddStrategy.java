package com.orange.strategy.impl;

import java.util.Arrays;
import java.util.Set;

import com.orange.model.StrategyConfig;
import com.orange.model.architecture.Architecture;
import com.orange.strategy.Strategy;
import com.orange.util.SetUtil;

public class RemoveAddStrategy extends Strategy {
    public RemoveAddStrategy(StrategyConfig config) {
	super(config);
	transitions = Arrays.asList(library.removeOldTransit, library.addNewTransit);
    }

    /**
     * This strategy only deal with additions and removals of microservice. i.e.
     * It should not exist microservice (identify by name) which is in both
     * currentArchitecture and finalArchitecture
     */
    @Override
    public boolean valid(Architecture currentArchitecture, Architecture finalArchitecture) {
	for (String site : finalArchitecture.listSitesName()) {
	    Set<String> currentMsName = SetUtil.collectNames(currentArchitecture.getSiteMicroservices(site));
	    Set<String> finalMsName = SetUtil.collectNames(finalArchitecture.getSiteMicroservices(site));
	    if (currentMsName.stream().anyMatch(finalMsName::contains)) {
		return false;
	    }
	}
	return true;
    }

}

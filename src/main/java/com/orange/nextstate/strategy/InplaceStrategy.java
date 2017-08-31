package com.orange.nextstate.strategy;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.model.StrategyConfig;
import com.orange.model.state.Architecture;
import com.orange.util.SetUtil;

public class InplaceStrategy extends Strategy {
    private static final Logger logger = LoggerFactory.getLogger(InplaceStrategy.class);

    public InplaceStrategy(StrategyConfig config) {
	super(config);
    }

    /**
     * If microservice name in finalState not unique, then return false. i.e.
     * This strategy only deal with the case that only one deployment version
     * (i.e. ArchitectureMicroservice) per microservice (identify by name).
     */
    @Override
    public boolean valid(Architecture currentState, Architecture finalState) {
	for (String site : finalState.listSitesName()) {
	    if (!SetUtil.uniqueByName(finalState.getArchitectureSite(site).getArchitectureMicroservices())) {
		logger.error("InplaceStrategy should be used for the case that multi versions in currentState.");
		return false;
	    }
	}
	return true;
    }

    @Override
    public List<Transit> transits() {
	return Arrays.asList(library.directTransit(false));
    }

}

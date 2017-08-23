package com.orange.midstate.strategy;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.model.StrategyConfig;
import com.orange.model.state.Overview;
import com.orange.util.SetUtil;

public class InplaceStrategy extends Strategy {
    private static final Logger logger = LoggerFactory.getLogger(InplaceStrategy.class);

    public InplaceStrategy(StrategyConfig config) {
	super(config);
    }

    /**
     * If app name in finalState not unique, then return false. i.e. This
     * strategy only deal with the case that only one deployment version (i.e.
     * OverviewApp) per microservice (identify by name).
     */
    @Override
    public boolean valid(Overview currentState, Overview finalState) {
	for (String site : finalState.listSitesName()) {
	    if (!SetUtil.uniqueByName(finalState.getOverviewSite(site).getOverviewApps())) {
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

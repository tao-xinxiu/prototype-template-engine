package com.orange.midstate.strategy;

import java.util.Iterator;

import org.slf4j.Logger;

import com.orange.model.StrategyConfig;
import com.orange.model.state.Overview;
import com.orange.model.state.OverviewApp;
import com.orange.util.SetUtil;

public class StrategyLibrary {
    private StrategyConfig config;
    private Logger logger;

    public StrategyLibrary(StrategyConfig config, Logger logger) {
	this.config = config;
	this.logger = logger;
    }

    protected TransitPoint removeUndesiredTransit = new TransitPoint() {
	@Override
	public boolean condition(Overview currentState, Overview finalState) {
	    for (String site : finalState.listSitesName()) {
		for (OverviewApp currentApp : currentState.getOverviewSite(site).getOverviewApps()) {
		    if (SetUtil.noneMatch(finalState.getOverviewSite(site).getOverviewApps(),
			    desiredApp -> currentApp.isInstantiation(desiredApp))) {
			logger.info("removeUndesiredTransit detected");
			return true;
		    }
		}
	    }
	    return false;
	}

	@Override
	public Overview next(Overview currentState, Overview finalState) {
	    logger.info("start getting next architecture by removing undesired microservice.");
	    Overview nextState = new Overview(currentState);
	    for (String site : finalState.listSitesName()) {
		Iterator<OverviewApp> iterator = nextState.getOverviewSite(site).getOverviewApps().iterator();
		while (iterator.hasNext()) {
		    OverviewApp app = iterator.next();
		    if (SetUtil.noneMatch(finalState.getOverviewSite(site).getOverviewApps(),
			    desiredApp -> app.isInstantiation(desiredApp))) {
			iterator.remove();
			logger.info("Removed microservice [{}]", app);
		    }
		}
	    }
	    return nextState;
	}
    };

}

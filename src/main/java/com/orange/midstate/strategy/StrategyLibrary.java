package com.orange.midstate.strategy;

import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.model.StrategyConfig;
import com.orange.model.state.AppState;
import com.orange.model.state.Overview;
import com.orange.model.state.OverviewApp;
import com.orange.util.SetUtil;

public class StrategyLibrary {
    private static final Logger logger = LoggerFactory.getLogger(StrategyLibrary.class);

    private StrategyConfig config;

    public StrategyLibrary(StrategyConfig config) {
	this.config = config;
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

    protected TransitPoint cleanNotUpdatableAppTransit = new TransitPoint() {
	// TODO for other PaaS (not CF), it's possible that createApp and setEnv
	// is not a single atomic operation. In this case, strategy need to
	// identify and cleanUp env not set apps.
	@Override
	public boolean condition(Overview currentState, Overview finalState) {
	    for (String site : finalState.listSitesName()) {
		// based on path = "" if null, during getCurrentState
		Set<OverviewApp> nonUpdatableApp = SetUtil.search(currentState.getOverviewSite(site).getOverviewApps(),
			app -> "".equals(app.getPath()) || app.getState().equals(AppState.CREATED));
		if (!nonUpdatableApp.isEmpty()) {
		    logger.info("cleanNotUpdatableAppTransit detected for microservices: {}", nonUpdatableApp);
		    return true;
		}
	    }
	    return false;
	}

	@Override
	public Overview next(Overview currentState, Overview finalState) {
	    Overview nextState = new Overview(currentState);
	    for (String site : finalState.listSitesName()) {
		// based on path = "" if null, during getCurrentState
		Set<OverviewApp> nextApps = nextState.getOverviewSite(site).getOverviewApps();
		Set<OverviewApp> nonUpdatableApp = SetUtil.search(nextApps,
			app -> "".equals(app.getPath()) || app.getState().equals(AppState.CREATED));
		if (!nonUpdatableApp.isEmpty()) {
		    logger.info("clean up not updatable microservices: {}", nonUpdatableApp);
		    nextApps.removeAll(nonUpdatableApp);
		}
	    }
	    return nextState;
	}
    };
}

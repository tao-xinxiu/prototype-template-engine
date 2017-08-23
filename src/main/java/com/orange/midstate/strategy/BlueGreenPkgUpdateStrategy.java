package com.orange.midstate.strategy;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.model.StrategyConfig;
import com.orange.model.state.Overview;
import com.orange.model.state.OverviewApp;
import com.orange.util.SetUtil;
import com.orange.util.VersionGenerator;

public class BlueGreenPkgUpdateStrategy extends Strategy {
    private static final Logger logger = LoggerFactory.getLogger(BlueGreenPkgUpdateStrategy.class);

    public BlueGreenPkgUpdateStrategy(StrategyConfig config) {
	super(config);
    }

    @Override
    public boolean valid(Overview currentState, Overview finalState) {
	return true;
    }

    @Override
    public List<Transit> transits() {
	return Arrays.asList(newPkgTransit, updateExceptPkgTransit, library.removeUndesiredTransit);
    }

    /**
     * getting next architecture by adding microservice with new pkg
     */
    protected Transit newPkgTransit = new Transit() {
	@Override
	public Overview next(Overview currentState, Overview finalState) {
	    Overview nextState = new Overview(currentState);
	    for (String site : finalState.listSitesName()) {
		for (OverviewApp desiredApp : finalState.getOverviewSite(site).getOverviewApps()) {
		    if (SetUtil.search(currentState.getOverviewSite(site).getOverviewApps(),
			    app -> app.getName().equals(desiredApp.getName())
				    && app.getPath().equals(desiredApp.getPath()))
			    .isEmpty()) {
			OverviewApp newApp = new OverviewApp(desiredApp);
			newApp.setGuid(null);
			newApp.setRoutes(library.tmpRoute(site, desiredApp));
			if (newApp.getVersion() == null) {
			    newApp.setVersion(config.getUpdatingVersion());
			}
			nextState.getOverviewSite(site).addOverviewApp(newApp);
			logger.info("Added a new microservice: {} ", newApp);
		    }
		}
	    }
	    return nextState;
	}
    };

    /**
     * getting next architecture by updating desired microservice properties
     */
    protected Transit updateExceptPkgTransit = new Transit() {
	// assume that it doesn't exist two apps with same pkg and name
	@Override
	public Overview next(Overview currentState, Overview finalState) {
	    Overview nextState = new Overview(currentState);
	    for (String site : finalState.listSitesName()) {
		for (OverviewApp desiredApp : finalState.getOverviewSite(site).getOverviewApps()) {
		    if (SetUtil.noneMatch(nextState.getOverviewSite(site).getOverviewApps(),
			    app -> app.isInstantiation(desiredApp))) {
			Set<OverviewApp> nextApps = nextState.getOverviewSite(site).getOverviewApps();
			OverviewApp nextApp = SetUtil.getOneApp(nextApps,
				app -> app.getName().equals(desiredApp.getName())
					&& app.getPath().equals(desiredApp.getPath()));
			nextApp.setRoutes(desiredApp.getRoutes());
			nextApp.setEnv(desiredApp.getEnv());
			nextApp.setNbProcesses(desiredApp.getNbProcesses());
			nextApp.setServices(desiredApp.getServices());
			nextApp.setState(desiredApp.getState());
			logger.info("Updated microservice [{}_{}] to {} ", nextApp.getName(), nextApp.getVersion(),
				nextApp);
		    }
		}
	    }
	    return nextState;
	}
    };
}

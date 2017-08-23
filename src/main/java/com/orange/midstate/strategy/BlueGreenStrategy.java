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

public class BlueGreenStrategy extends Strategy {
    private static final Logger logger = LoggerFactory.getLogger(BlueGreenStrategy.class);

    public BlueGreenStrategy(StrategyConfig config) {
	super(config);
    }

    @Override
    public boolean valid(Overview currentState, Overview finalState) {
	// TODO in the case of multi new versions, version should be specified
	// in finalState
	return true;
    }

    @Override
    public List<Transit> transits() {
	return Arrays.asList(newPkgEnvTransit, updateExceptRouteTransit, library.updateRouteTransit,
		library.removeUndesiredTransit);
    }

    /**
     * getting next architecture by adding microservice with new pkg and env.
     * Tagging updating microservice with version "updating"
     */
    protected Transit newPkgEnvTransit = new Transit() {
	@Override
	public Overview next(Overview currentState, Overview finalState) {
	    Overview nextState = new Overview(currentState);
	    for (String site : finalState.listSitesName()) {
		Set<OverviewApp> currentApps = nextState.getOverviewSite(site).getOverviewApps();
		for (OverviewApp desiredApp : finalState.getOverviewSite(site).getOverviewApps()) {
		    if (SetUtil.noneMatch(currentApps, app -> app.getName().equals(desiredApp.getName())
			    && app.getVersion().equals(library.desiredVersion(desiredApp)))) {
			OverviewApp newApp = new OverviewApp(desiredApp);
			newApp.setGuid(null);
			newApp.setRoutes(library.tmpRoute(site, desiredApp));
			if (newApp.getVersion() == null) {
			    newApp.setVersion(config.getUpdatingVersion());
			}
			nextState.getOverviewSite(site).addOverviewApp(newApp);
			logger.info("Added a new microservice: {} ", newApp);
			continue;
		    }
		}
	    }
	    return nextState;
	}
    };

    /**
     * getting next architecture by updating desired microservice properties
     * (except route)
     */
    protected Transit updateExceptRouteTransit = new Transit() {
	// assume that it doesn't exist two apps with same pkg and name
	@Override
	public Overview next(Overview currentState, Overview finalState) {
	    Overview nextState = new Overview(currentState);
	    for (String site : finalState.listSitesName()) {
		for (OverviewApp desiredApp : finalState.getOverviewSite(site).getOverviewApps()) {
		    if (SetUtil.noneMatch(nextState.getOverviewSite(site).getOverviewApps(),
			    app -> app.getName().equals(desiredApp.getName())
				    && app.getVersion().equals(library.desiredVersion(desiredApp))
				    && app.getPath().equals(desiredApp.getPath())
				    && app.getEnv().equals(desiredApp.getEnv())
				    && app.getNbProcesses() == desiredApp.getNbProcesses()
				    && app.getServices().equals(desiredApp.getServices())
				    && app.getState().equals(desiredApp.getState()))) {
			OverviewApp nextApp = SetUtil.getOneApp(nextState.getOverviewSite(site).getOverviewApps(),
				app -> app.getName().equals(desiredApp.getName())
					&& app.getVersion().equals(library.desiredVersion(desiredApp)));
			nextApp.setPath(desiredApp.getPath());
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

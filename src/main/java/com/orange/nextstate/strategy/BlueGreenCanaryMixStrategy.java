package com.orange.nextstate.strategy;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.model.StrategyConfig;
import com.orange.model.state.Overview;
import com.orange.model.state.OverviewApp;
import com.orange.util.SetUtil;

public class BlueGreenCanaryMixStrategy extends Strategy {
    private static final Logger logger = LoggerFactory.getLogger(BlueGreenCanaryMixStrategy.class);

    public BlueGreenCanaryMixStrategy(StrategyConfig config) {
	super(config);
    }

    @Override
    public boolean valid(Overview currentState, Overview finalState) {
	return true;
    }

    @Override
    public List<Transit> transits() {
	return Arrays.asList(addCanaryTransit, updateExceptInstancesRoutesTransit, updateRouteTransit, rolloutTransit,
		library.removeUndesiredTransit);
    }

    /**
     * next architecture: add canary microservice with new pkg and env
     */
    protected Transit addCanaryTransit = new Transit() {
	@Override
	public Overview next(Overview currentState, Overview finalState) {
	    Overview nextState = new Overview(currentState);
	    for (String site : finalState.listSitesName()) {
		Set<OverviewApp> currentApps = nextState.getOverviewSite(site).getOverviewApps();
		for (OverviewApp desiredApp : finalState.getOverviewSite(site).getOverviewApps()) {
		    if (SetUtil.noneMatch(currentApps,
			    app -> app.getName().equals(desiredApp.getName())
				    && app.getPath().equals(desiredApp.getPath())
				    && app.getEnv().equals(desiredApp.getEnv()))) {
			OverviewApp newApp = new OverviewApp(desiredApp);
			newApp.setGuid(null);
			if (newApp.getVersion() == null) {
			    newApp.setVersion(config.getUpdatingVersion());
			}
			newApp.setRoutes(library.tmpRoute(site, desiredApp));
			newApp.setNbProcesses(config.getCanaryNbr());
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
     * next architecture: update desired microservice properties except
     * nbrProcesses and routes
     */
    protected Transit updateExceptInstancesRoutesTransit = new Transit() {
	// assume that it doesn't exist two apps with same pkg and name
	@Override
	public Overview next(Overview currentState, Overview finalState) {
	    Overview nextState = new Overview(currentState);
	    for (String site : finalState.listSitesName()) {
		for (OverviewApp desiredApp : finalState.getOverviewSite(site).getOverviewApps()) {
		    if (SetUtil.noneMatch(nextState.getOverviewSite(site).getOverviewApps(),
			    app -> app.getName().equals(desiredApp.getName())
				    && app.getVersion().equals(config.getUpdatingVersion())
				    && app.getPath().equals(desiredApp.getPath())
				    && app.getEnv().equals(desiredApp.getEnv())
				    && app.getServices().equals(desiredApp.getServices())
				    && app.getState().equals(desiredApp.getState()))) {
			OverviewApp nextApp = SetUtil.getUniqueApp(nextState.getOverviewSite(site).getOverviewApps(),
				app -> app.getName().equals(desiredApp.getName())
					&& app.getVersion().equals(config.getUpdatingVersion()));
			nextApp.setPath(desiredApp.getPath());
			nextApp.setEnv(desiredApp.getEnv());
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

    /**
     * next architecture: update desired microservice route
     */
    protected Transit updateRouteTransit = new Transit() {
	// assume that it doesn't exist two apps with same pkg and name
	@Override
	public Overview next(Overview currentState, Overview finalState) {
	    Overview nextState = new Overview(currentState);
	    for (String site : finalState.listSitesName()) {
		for (OverviewApp desiredApp : finalState.getOverviewSite(site).getOverviewApps()) {
		    if (SetUtil.noneMatch(nextState.getOverviewSite(site).getOverviewApps(),
			    app -> app.getName().equals(desiredApp.getName())
				    && app.getVersion().equals(config.getUpdatingVersion())
				    && app.getPath().equals(desiredApp.getPath())
				    && app.getEnv().equals(desiredApp.getEnv())
				    && app.getServices().equals(desiredApp.getServices())
				    && app.getState().equals(desiredApp.getState())
				    && app.getRoutes().equals(desiredApp.getRoutes()))) {
			OverviewApp nextApp = SetUtil.getUniqueApp(nextState.getOverviewSite(site).getOverviewApps(),
				app -> app.getName().equals(desiredApp.getName())
					&& app.getVersion().equals(config.getUpdatingVersion()));
			nextApp.setRoutes(desiredApp.getRoutes());
			logger.info("Updated microservice [{}_{}] route to {} ", nextApp.getName(),
				nextApp.getVersion(), nextApp.getRoutes());
		    }
		}
	    }
	    return nextState;
	}
    };

    /**
     * next architecture: scale up desired microservice and rollout old ones
     */
    protected Transit rolloutTransit = new Transit() {
	@Override
	public Overview next(Overview currentState, Overview finalState) {
	    Overview nextState = new Overview(currentState);
	    for (String site : finalState.listSitesName()) {
		for (OverviewApp desiredApp : finalState.getOverviewSite(site).getOverviewApps()) {
		    Set<OverviewApp> nextApps = nextState.getOverviewSite(site).getOverviewApps();
		    if (SetUtil.noneMatch(nextApps, app -> app.isInstantiation(desiredApp))) {
			for (OverviewApp nextApp : SetUtil.searchByName(nextApps, desiredApp.getName())) {
			    if (nextApp.getVersion().equals(library.desiredVersion(desiredApp))) {
				int nextNbr = nextApp.getNbProcesses() + config.getCanaryIncrease();
				if (nextNbr > desiredApp.getNbProcesses()) {
				    nextNbr = desiredApp.getNbProcesses();
				}
				nextApp.setNbProcesses(nextNbr);
			    } else {
				int nextNbr = nextApp.getNbProcesses() - config.getCanaryIncrease();
				if (nextNbr < 1) {
				    nextNbr = 1;
				}
				nextApp.setNbProcesses(nextNbr);
			    }
			}
		    }
		}
	    }
	    return nextState;
	}
    };

}

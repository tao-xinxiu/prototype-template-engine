package com.orange.midstate.strategy;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.model.StrategyConfig;
import com.orange.model.state.AppState;
import com.orange.model.state.Overview;
import com.orange.model.state.OverviewApp;
import com.orange.util.SetUtil;
import com.orange.util.VersionGenerator;

public class StrategyLibrary {
    private static final Logger logger = LoggerFactory.getLogger(StrategyLibrary.class);

    private StrategyConfig config;

    public StrategyLibrary(StrategyConfig config) {
	this.config = config;
    }

    /**
     * next architecture: remove micro-services not in finalState
     */
    protected Transit removeUndesiredTransit = new Transit() {
	@Override
	public Overview next(Overview currentState, Overview finalState) {
	    Overview nextState = new Overview(currentState);
	    for (String site : finalState.listSitesName()) {
		Set<OverviewApp> nextApps = nextState.getOverviewSite(site).getOverviewApps();
		Iterator<OverviewApp> iterator = nextApps.iterator();
		while (iterator.hasNext()) {
		    OverviewApp app = iterator.next();
		    if (SetUtil.noneMatch(finalState.getOverviewSite(site).getOverviewApps(),
			    desiredApp -> app.isInstantiation(desiredApp))) {
			iterator.remove();
			logger.info("Removed microservice [{}]", app);
		    } else if (app.getVersion().equals(config.getUpdatingVersion())) {
			app.setVersion(VersionGenerator.random(SetUtil.collectVersions(nextApps)));
		    }
		}
	    }
	    return nextState;
	}
    };

    /**
     * next architecture: adding new micro-services (in finalState, not in
     * currentState)
     */
    protected Transit addNewTransit = new Transit() {
	@Override
	public Overview next(Overview currentState, Overview finalState) {
	    Overview nextState = new Overview(currentState);
	    for (String site : finalState.listSitesName()) {
		Set<OverviewApp> currentApps = nextState.getOverviewSite(site).getOverviewApps();
		for (OverviewApp desiredApp : finalState.getOverviewSite(site).getOverviewApps()) {
		    if (SetUtil.noneMatch(currentApps, app -> app.getName().equals(desiredApp.getName()))) {
			OverviewApp newApp = new OverviewApp(desiredApp);
			newApp.setGuid(null);
			newApp.setVersion(VersionGenerator.random(new HashSet<>()));
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
     * next architecture: direct in-place update current most similar
     * microservice to desired
     * 
     * @param tmpRoute
     *            whether map new or path/env updated microservices to a
     *            temporary route
     * @return
     */
    protected Transit directTransit(boolean tmpRoute) {
	return new Transit() {
	    @Override
	    public Overview next(Overview currentState, Overview finalState) {
		Overview nextState = new Overview(finalState);
		for (String site : finalState.listSitesName()) {
		    for (OverviewApp nextApp : nextState.getOverviewSite(site).getOverviewApps()) {
			Set<OverviewApp> currentApps = SetUtil
				.searchByName(currentState.getOverviewSite(site).getOverviewApps(), nextApp.getName());
			if (currentApps.size() == 0) {
			    // Add non-exist microservice
			    nextApp.setGuid(null);
			    nextApp.setVersion(VersionGenerator.random(new HashSet<>()));
			    if (tmpRoute) {
				nextApp.setRoutes(tmpRoute(site, nextApp));
			    }
			    logger.info("{} detected as a new microservice.", nextApp);
			} else {
			    // update from most similar microservice (i.e.
			    // prefer path and env equals if exist)
			    OverviewApp currentApp = SetUtil.getOneApp(currentApps,
				    app -> app.getPath().equals(nextApp.getPath())
					    && app.getEnv().equals(nextApp.getEnv()));
			    if (currentApp == null) {
				currentApp = currentApps.iterator().next();
				if (tmpRoute) {
				    nextApp.setRoutes(tmpRoute(site, nextApp));
				}
			    }
			    nextApp.setGuid(currentApp.getGuid());
			    nextApp.setVersion(currentApp.getVersion());
			    logger.info("{} detected as a updated microservice", nextApp);
			}
		    }
		}
		return nextState;
	    }
	};
    }

    /**
     * getting next architecture by updating desired microservice route and
     * setting version
     */
    protected Transit updateRouteTransit = new Transit() {
	// assume that it doesn't exist two apps with same pkg and name
	@Override
	public Overview next(Overview currentState, Overview finalState) {
	    Overview nextState = new Overview(currentState);
	    for (String site : finalState.listSitesName()) {
		for (OverviewApp desiredApp : finalState.getOverviewSite(site).getOverviewApps()) {
		    Set<OverviewApp> nextApps = SetUtil.searchByName(nextState.getOverviewSite(site).getOverviewApps(),
			    desiredApp.getName());
		    if (SetUtil.noneMatch(nextApps, app -> app.isInstantiation(desiredApp))) {
			OverviewApp nextApp = SetUtil.getOneApp(nextApps,
				app -> app.getVersion().equals(desiredVersion(desiredApp))
					&& app.getPath().equals(desiredApp.getPath())
					&& app.getEnv().equals(desiredApp.getEnv())
					&& app.getNbProcesses() == desiredApp.getNbProcesses()
					&& app.getServices().equals(desiredApp.getServices())
					&& app.getState().equals(desiredApp.getState()));
			nextApp.setRoutes(desiredApp.getRoutes());
			logger.info("Updated microservice [{}_{}] route to {} ", nextApp.getName(),
				nextApp.getVersion(), nextApp.getRoutes());
		    }
		}
	    }
	    return nextState;
	}
    };

    protected Transit cleanAllTransit = new Transit() {
	@Override
	public Overview next(Overview currentState, Overview finalState) {
	    Overview nextState = new Overview(currentState);
	    nextState.getOverviewSites().values().stream().forEach(s -> s.getOverviewApps().clear());
	    return nextState;
	}
    };

    protected Transit deployAllTransit = new Transit() {
	@Override
	public Overview next(Overview currentState, Overview finalState) {
	    Overview nextState = new Overview(currentState);
	    for (String site : finalState.listSitesName()) {
		Set<OverviewApp> nextApps = nextState.getOverviewSite(site).getOverviewApps();
		if (nextApps.isEmpty()) {
		    nextApps.addAll(finalState.getOverviewSite(site).getOverviewApps());
		}
	    }
	    return nextState;
	}
    };

    protected Transit cleanNotUpdatableAppTransit = new Transit() {
	// TODO for other PaaS (not CF), it's possible that createApp and setEnv
	// is not a single atomic operation. In this case, strategy need to
	// identify and cleanUp env not set apps.
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

    public String desiredVersion(OverviewApp desiredApp) {
	return desiredApp.getVersion() == null ? config.getUpdatingVersion() : desiredApp.getVersion();
    }

    public Set<String> tmpRoute(String site, OverviewApp app) {
	return Collections.singleton(config.getSiteConfig(site).getTmpRoute(app.getName()));
    }
}

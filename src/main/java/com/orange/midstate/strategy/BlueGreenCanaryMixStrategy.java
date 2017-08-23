package com.orange.midstate.strategy;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.model.StrategyConfig;
import com.orange.model.StrategySiteConfig;
import com.orange.model.state.Overview;
import com.orange.model.state.OverviewApp;
import com.orange.util.SetUtil;
import com.orange.util.VersionGenerator;

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
	return Arrays.asList(addCanaryTransit, updateServiceStateTransit, updateRouteTransit, rolloutTransit,
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
		Set<String> usedVersions = currentApps.stream().map(app -> app.getInstanceVersion())
			.collect(Collectors.toSet());
		StrategySiteConfig siteConfig = (StrategySiteConfig) (config.getSiteConfig(site));
		for (OverviewApp desiredApp : finalState.getOverviewSite(site).getOverviewApps()) {
		    if (SetUtil.noneMatch(currentApps,
			    app -> app.getName().equals(desiredApp.getName())
				    && app.getPath().equals(desiredApp.getPath())
				    && app.getEnv().equals(desiredApp.getEnv()))) {
			OverviewApp newApp = new OverviewApp(desiredApp);
			newApp.setGuid(null);
			newApp.setInstanceVersion(VersionGenerator.random(usedVersions));
			usedVersions.add(newApp.getInstanceVersion());
			newApp.setRoutes(Collections.singleton(siteConfig.getTmpRoute(desiredApp.getName())));
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
     * nbrProcesses
     */
    protected Transit updateServiceStateTransit = new Transit() {
	// assume that it doesn't exist two apps with same pkg and name
	@Override
	public Overview next(Overview currentState, Overview finalState) {
	    Overview nextState = new Overview(currentState);
	    for (String site : finalState.listSitesName()) {
		for (OverviewApp desiredApp : finalState.getOverviewSite(site).getOverviewApps()) {
		    if (SetUtil.noneMatch(nextState.getOverviewSite(site).getOverviewApps(),
			    app -> app.getName().equals(desiredApp.getName())
				    && app.getPath().equals(desiredApp.getPath())
				    && app.getEnv().equals(desiredApp.getEnv())
				    && app.getServices().equals(desiredApp.getServices())
				    && app.getState().equals(desiredApp.getState()))) {
			OverviewApp nextApp = SetUtil.getUniqueApp(nextState.getOverviewSite(site).getOverviewApps(),
				app -> app.getName().equals(desiredApp.getName())
					&& app.getPath().equals(desiredApp.getPath())
					&& app.getEnv().equals(desiredApp.getEnv()));
			nextApp.setServices(desiredApp.getServices());
			nextApp.setState(desiredApp.getState());
			logger.info("Updated microservice [{}_{}] to {} ", nextApp.getName(),
				nextApp.getInstanceVersion(), nextApp);
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
				    && app.getPath().equals(desiredApp.getPath())
				    && app.getEnv().equals(desiredApp.getEnv())
				    && app.getServices().equals(desiredApp.getServices())
				    && app.getState().equals(desiredApp.getState())
				    && app.getRoutes().equals(desiredApp.getRoutes()))) {
			OverviewApp nextApp = SetUtil.getUniqueApp(nextState.getOverviewSite(site).getOverviewApps(),
				app -> app.getName().equals(desiredApp.getName())
					&& app.getPath().equals(desiredApp.getPath())
					&& app.getEnv().equals(desiredApp.getEnv()));
			nextApp.setRoutes(desiredApp.getRoutes());
			logger.info("Updated microservice [{}_{}] route to {} ", nextApp.getName(),
				nextApp.getInstanceVersion(), nextApp.getRoutes());
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
			    if (nextApp.getPath().equals(desiredApp.getPath())
				    && nextApp.getEnv().equals(desiredApp.getEnv())) {
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

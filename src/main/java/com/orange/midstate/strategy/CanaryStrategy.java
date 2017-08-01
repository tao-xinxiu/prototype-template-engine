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

public class CanaryStrategy extends Strategy {
    private static final Logger logger = LoggerFactory.getLogger(CanaryStrategy.class);

    public CanaryStrategy(StrategyConfig config) {
	super(config);
    }

    @Override
    public boolean valid(Overview currentState, Overview finalState) {
	return true;
    }

    @Override
    public List<TransitPoint> transitPoints() {
	return Arrays.asList(addCanaryTransit, updateExceptPkgEnvNbrTransit, rolloutTransit,
		new StrategyLibrary(config, logger).removeUndesiredTransit);
    }

    protected TransitPoint addCanaryTransit = new TransitPoint() {
	// TODO similar/duplicate to BlueGreenStrategy, add into Strategy lib
	@Override
	public boolean condition(Overview currentState, Overview finalState) {
	    for (String site : finalState.listSitesName()) {
		for (OverviewApp desiredApp : finalState.getOverviewSite(site).getOverviewApps()) {
		    if (SetUtil.noneMatch(currentState.getOverviewSite(site).getOverviewApps(),
			    app -> app.getName().equals(desiredApp.getName())
				    && app.getPath().equals(desiredApp.getPath())
				    && app.getEnv().equals(desiredApp.getEnv()))) {
			logger.info("addCanaryTransit detected for microservice {}", desiredApp);
			return true;
		    }
		}
	    }
	    return false;
	}

	@Override
	public Overview next(Overview currentState, Overview finalState) {
	    logger.info("start getting next architecture by adding canary microservice with new pkg and env");
	    Overview nextState = new Overview(currentState);
	    for (String site : finalState.listSitesName()) {
		Set<OverviewApp> currentApps = currentState.getOverviewSite(site).getOverviewApps();
		// TODO fix instanceVersion doesn't need to be unique for all
		// apps.
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
			newApp.setRoutes(Collections.singleton(siteConfig.getTmpRoute(desiredApp.getName())));
			newApp.setInstanceVersion(VersionGenerator.random(usedVersions));
			newApp.setNbProcesses(config.getCanaryNbr());
			usedVersions.add(newApp.getInstanceVersion());
			nextState.getOverviewSite(site).addOverviewApp(newApp);
			logger.info("Added a new microservice: {} ", newApp);
			continue;
		    }
		}
	    }
	    return nextState;
	}
    };

    protected TransitPoint updateExceptPkgEnvNbrTransit = new TransitPoint() {
	@Override
	public boolean condition(Overview currentState, Overview finalState) {
	    for (String site : finalState.listSitesName()) {
		for (OverviewApp desiredApp : finalState.getOverviewSite(site).getOverviewApps()) {
		    if (SetUtil.noneMatch(currentState.getOverviewSite(site).getOverviewApps(),
			    app -> app.getName().equals(desiredApp.getName())
				    && app.getPath().equals(desiredApp.getPath())
				    && app.getEnv().equals(desiredApp.getEnv())
				    && app.getRoutes().equals(desiredApp.getRoutes())
				    && app.getServices().equals(desiredApp.getServices())
				    && app.getState().equals(desiredApp.getState()))) {
			logger.info("updateExceptPkgEnvNbrTransit detected for microservice {}", desiredApp);
			return true;
		    }
		}
	    }
	    return false;
	}

	// assume that it doesn't exist two apps with same pkg and name
	@Override
	public Overview next(Overview currentState, Overview finalState) {
	    logger.info(
		    "start getting next architecture by updating desired microservice properties except nbrProcesses");
	    Overview nextState = new Overview(currentState);
	    for (String site : finalState.listSitesName()) {
		for (OverviewApp desiredApp : finalState.getOverviewSite(site).getOverviewApps()) {
		    if (SetUtil.noneMatch(nextState.getOverviewSite(site).getOverviewApps(),
			    app -> app.getName().equals(desiredApp.getName())
				    && app.getPath().equals(desiredApp.getPath())
				    && app.getEnv().equals(desiredApp.getEnv())
				    && app.getRoutes().equals(desiredApp.getRoutes())
				    && app.getServices().equals(desiredApp.getServices())
				    && app.getState().equals(desiredApp.getState()))) {
			OverviewApp nextApp = SetUtil.getOneApp(nextState.getOverviewSite(site).getOverviewApps(),
				app -> app.getName().equals(desiredApp.getName())
					&& app.getPath().equals(desiredApp.getPath())
					&& app.getEnv().equals(desiredApp.getEnv()));
			nextApp.setRoutes(desiredApp.getRoutes());
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

    protected TransitPoint rolloutTransit = new TransitPoint() {
	@Override
	public boolean condition(Overview currentState, Overview finalState) {
	    for (String site : finalState.listSitesName()) {
		for (OverviewApp desiredApp : finalState.getOverviewSite(site).getOverviewApps()) {
		    if (SetUtil.noneMatch(currentState.getOverviewSite(site).getOverviewApps(),
			    app -> app.isInstantiation(desiredApp))) {
			logger.info("rolloutTransit detected");
			return true;
		    }
		}
	    }
	    return false;
	}

	@Override
	public Overview next(Overview currentState, Overview finalState) {
	    logger.info("start getting next architecture by scale up desired microservice and rollout old ones");
	    Overview nextState = new Overview(currentState);
	    for (String site : finalState.listSitesName()) {
		for (OverviewApp desiredApp : finalState.getOverviewSite(site).getOverviewApps()) {
		    for (OverviewApp nextApp : SetUtil.searchByName(nextState.getOverviewSite(site).getOverviewApps(),
			    desiredApp.getName())) {
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
	    return nextState;
	}
    };

}

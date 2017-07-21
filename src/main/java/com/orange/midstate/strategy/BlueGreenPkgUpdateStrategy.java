package com.orange.midstate.strategy;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.model.StrategySiteConfig;
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
    public List<TransitPoint> transitPoints() {
	return Arrays.asList(newPkgTransit, updateExceptPkgTransit, removeUndesiredTransit);
    }

    protected TransitPoint newPkgTransit = new TransitPoint() {
	@Override
	public boolean condition(Overview currentState, Overview finalState) {
	    for (String site : finalState.listSitesName()) {
		for (OverviewApp desiredApp : finalState.getOverviewSite(site).getOverviewApps()) {
		    if (SetUtil.search(currentState.getOverviewSite(site).getOverviewApps(),
			    app -> app.getName().equals(desiredApp.getName())
				    && app.getPath().equals(desiredApp.getPath()))
			    .isEmpty()) {
			logger.info("newPkgTransit detected");
			return true;
		    }
		}
	    }
	    return false;
	}

	@Override
	public Overview next(Overview currentState, Overview finalState) {
	    logger.info("start getting next architecture by adding microservice with new pkg");
	    Overview nextState = new Overview(currentState);
	    for (String site : finalState.listSitesName()) {
		Set<String> usedVersions = currentState.getOverviewSite(site).getOverviewApps().stream()
			.map(app -> app.getInstanceVersion()).collect(Collectors.toSet());
		StrategySiteConfig siteConfig = config.getSiteConfig(site);
		for (OverviewApp desiredApp : finalState.getOverviewSite(site).getOverviewApps()) {
		    if (SetUtil.search(currentState.getOverviewSite(site).getOverviewApps(),
			    app -> app.getName().equals(desiredApp.getName())
				    && app.getPath().equals(desiredApp.getPath()))
			    .isEmpty()) {
			OverviewApp newApp = new OverviewApp(desiredApp);
			newApp.setGuid(null);
			newApp.setRoutes(Collections.singleton(siteConfig.getTmpRoute(desiredApp.getName())));
			newApp.setInstanceVersion(VersionGenerator.random(usedVersions));
			usedVersions.add(newApp.getInstanceVersion());
			nextState.getOverviewSite(site).addOverviewApp(newApp);
			logger.info("Added a new microservice: {} ", newApp);
		    }
		}
	    }
	    return nextState;
	}
    };

    protected TransitPoint updateExceptPkgTransit = new TransitPoint() {
	@Override
	public boolean condition(Overview currentState, Overview finalState) {
	    for (String site : finalState.listSitesName()) {
		for (OverviewApp desiredApp : finalState.getOverviewSite(site).getOverviewApps()) {
		    if (SetUtil.search(currentState.getOverviewSite(site).getOverviewApps(),
			    app -> app.isInstantiation(desiredApp)).isEmpty()) {
			logger.info("updateExceptPkgTransit detected");
			return true;
		    }
		}
	    }
	    return false;
	}

	// assume that it doesn't exist two apps with same pkg and name
	@Override
	public Overview next(Overview currentState, Overview finalState) {
	    logger.info("start getting next architecture by updating desired microservice properties");
	    Overview nextState = new Overview(currentState);
	    for (String site : finalState.listSitesName()) {
		for (OverviewApp desiredApp : finalState.getOverviewSite(site).getOverviewApps()) {
		    for (OverviewApp nextApp : nextState.getOverviewSite(site).getOverviewApps()) {
			if (nextApp.getName().equals(desiredApp.getName())
				&& nextApp.getPath().equals(desiredApp.getPath())) {
			    if (!nextApp.isInstantiation(desiredApp)) {
				nextApp.setRoutes(desiredApp.getRoutes());
				nextApp.setEnv(desiredApp.getEnv());
				nextApp.setNbProcesses(desiredApp.getNbProcesses());
				nextApp.setServices(desiredApp.getServices());
				nextApp.setState(desiredApp.getState());
				logger.info("Updated microservice [{}_{}] to {} ", nextApp.getName(),
					nextApp.getInstanceVersion(), nextApp);
			    }
			}
		    }
		}
	    }
	    return nextState;
	}
    };

    protected TransitPoint removeUndesiredTransit = new TransitPoint() {
	@Override
	public boolean condition(Overview currentState, Overview finalState) {
	    for (String site : finalState.listSitesName()) {
		for (OverviewApp currentApp : currentState.getOverviewSite(site).getOverviewApps()) {
		    if (SetUtil.search(finalState.getOverviewSite(site).getOverviewApps(),
			    desiredApp -> currentApp.isInstantiation(desiredApp)).isEmpty()) {
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
		    if (SetUtil.search(finalState.getOverviewSite(site).getOverviewApps(),
			    desiredApp -> app.isInstantiation(desiredApp)).isEmpty()) {
			iterator.remove();
			logger.info("Removed microservice [{}]", app);
		    }
		}
	    }
	    return nextState;
	}
    };
}

package com.orange.midstate.strategy;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.orange.model.StrategySiteConfig;
import com.orange.model.StrategyConfig;
import com.orange.model.state.Overview;
import com.orange.model.state.OverviewApp;
import com.orange.util.SetUtil;
import com.orange.util.VersionGenerator;

public class BlueGreenStrategy extends BlueGreenPkgUpdateStrategy {
    public BlueGreenStrategy(StrategyConfig config) {
	super(config);
    }

    @Override
    public List<TransitPoint> transitPoints() {
	return Arrays.asList(pkgEnvUpdateTransit, routeUpdateTransit, removeUndesiredTransit);
    }

    protected TransitPoint pkgEnvUpdateTransit = new TransitPoint() {
	@Override
	public boolean condition(Overview currentState, Overview finalState) {
	    for (String site : finalState.listSitesName()) {
		for (OverviewApp desiredApp : finalState.getOverviewSite(site).getOverviewApps()) {
		    if (SetUtil
			    .search(currentState.getOverviewSite(site).getOverviewApps(),
				    app -> app.getName().equals(desiredApp.getName())
					    && app.getPath().equals(desiredApp.getPath()))
			    .isEmpty()
			    || SetUtil.search(currentState.getOverviewSite(site).getOverviewApps(),
				    app -> app.getName().equals(desiredApp.getName())
					    && app.getEnv().equals(desiredApp.getEnv()))
				    .isEmpty()) {
			return true;
		    }
		}
	    }
	    return false;
	}

	@Override
	public Overview next(Overview currentState, Overview finalState) {
	    Overview nextState = new Overview(currentState);
	    for (String site : finalState.listSitesName()) {
		Set<String> usedVersions = currentState.getOverviewSite(site).getOverviewApps().stream()
			.map(app -> app.getInstanceVersion()).collect(Collectors.toSet());
		StrategySiteConfig siteConfig = (StrategySiteConfig) (config.getSiteConfig(site));
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
			continue;
		    }
		    if (SetUtil.search(currentState.getOverviewSite(site).getOverviewApps(),
			    app -> app.getName().equals(desiredApp.getName())
				    && app.getEnv().equals(desiredApp.getEnv()))
			    .isEmpty()) {
			OverviewApp newApp = new OverviewApp(desiredApp);
			newApp.setGuid(null);
			newApp.setRoutes(Collections.singleton(siteConfig.getTmpRoute(desiredApp.getName())));
			newApp.setInstanceVersion(VersionGenerator.random(usedVersions));
			usedVersions.add(newApp.getInstanceVersion());
			nextState.getOverviewSite(site).addOverviewApp(newApp);
		    }
		}
	    }
	    return nextState;
	}
    };
}

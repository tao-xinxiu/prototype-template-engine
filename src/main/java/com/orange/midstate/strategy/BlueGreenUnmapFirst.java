package com.orange.midstate.strategy;

import java.util.HashSet;
import java.util.Set;

import com.orange.model.SiteDeploymentConfig;
import com.orange.model.state.OverviewApp;

public class BlueGreenUnmapFirst extends BlueGreen {
	public BlueGreenUnmapFirst(Set<OverviewApp> updateApps, OverviewApp desiredApp, SiteDeploymentConfig config) {
		super(updateApps, desiredApp, config);
	}

	@Override
	public Set<OverviewApp> onRoutesUpdated() {
		Set<OverviewApp> desiredRelatedApps = Util.deepCopy(currentRelatedApps);
		OverviewApp instantiatedDesiredApp = instantiatedDesiredApp(desiredRelatedApps);
		Set<OverviewApp> oldApps = Util.exludedApps(desiredRelatedApps, instantiatedDesiredApp);
		boolean oldAppsUnmapped = true;
		for (OverviewApp app : oldApps) {
			if (!app.getRoutes().isEmpty()) {
				oldAppsUnmapped = false;
				app.setRoutes(new HashSet<>());
			}
		}
		if (oldAppsUnmapped) {
			instantiatedDesiredApp.setRoutes(desiredApp.getRoutes());
		}
		return desiredRelatedApps;
	}
}

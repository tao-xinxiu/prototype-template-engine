package com.orange.midstate.strategy;

import java.util.Set;

import com.orange.model.SiteDeploymentConfig;
import com.orange.model.state.AppState;
import com.orange.model.state.OverviewApp;

public class CanaryEconomic extends Canary {
	public CanaryEconomic(Set<OverviewApp> updateApps, OverviewApp desiredApp, SiteDeploymentConfig config) {
		super(updateApps, desiredApp, config);
		if (currentRelatedApps.size() > 2) {
			throw new IllegalStateException(
					"Strategy unsupported situation: more than two versions of the app coexist");
		}
	}

	@Override
	public Set<OverviewApp> onPathUpdated() {
		Set<OverviewApp> desiredRelatedApps = Util.deepCopy(currentRelatedApps);
		OverviewApp instantiatedDesiredApp = instantiatedDesiredApp(desiredRelatedApps);
		Set<OverviewApp> oldApps = Util.exludedApps(desiredRelatedApps, instantiatedDesiredApp);
		if (oldApps.size() == 0) { // initial deploy
			desiredRelatedApps.add(new OverviewApp(null, newAppName(), desiredApp.getPath(), AppState.RUNNING, 1,
					desiredApp.getEnv(), appTmpRoute()));
		} else {
			OverviewApp oldApp = oldApps.iterator().next();
			boolean oldAppsScaled = (oldApp.getInstances() == (desiredApp.getInstances() - 1));
			if (oldAppsScaled) {
				desiredRelatedApps.add(new OverviewApp(null, newAppName(), desiredApp.getPath(), AppState.RUNNING, 1,
						desiredApp.getEnv(), appTmpRoute()));
			} else {
				oldApp.setInstances(desiredApp.getInstances() - 1);
			}
		}
		return desiredRelatedApps;
	}

	@Override
	public Set<OverviewApp> onInstancesUpdated() {
		Set<OverviewApp> desiredRelatedApps = Util.deepCopy(currentRelatedApps);
		OverviewApp instantiatedDesiredApp = instantiatedDesiredApp(desiredRelatedApps);
		Set<OverviewApp> oldApps = Util.exludedApps(desiredRelatedApps, instantiatedDesiredApp);
		if (oldApps.size() == 0) { // initial deploy
			instantiatedDesiredApp.setInstances(desiredApp.getInstances());
		} else {
			OverviewApp oldApp = oldApps.iterator().next();
			boolean oldAppsScaled = (oldApp.getInstances()
					+ instantiatedDesiredApp.getInstances() == (desiredApp.getInstances() - 1));
			if (oldAppsScaled) {
				instantiatedDesiredApp.setInstances(instantiatedDesiredApp.getInstances() + 1);
			} else {
				oldApp.setInstances(oldApp.getInstances() - 1);
			}
		}
		return desiredRelatedApps;
	}
}

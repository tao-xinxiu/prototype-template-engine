package com.orange.midstate.strategy;

import java.util.Set;

import com.orange.model.SiteDeploymentConfig;
import com.orange.model.state.AppState;
import com.orange.model.state.OverviewApp;

public class Canary extends AppUpdateStrategy {
	public Canary(Set<OverviewApp> updateApps, OverviewApp desiredApp, SiteDeploymentConfig config) {
		super(updateApps, desiredApp, config);
	}

	@Override
	public Set<OverviewApp> onEnvUpdated() {
		Set<OverviewApp> desiredRelatedApps = Util.deepCopy(currentRelatedApps);
		desiredRelatedApps.add(new OverviewApp(null, newAppName(), desiredApp.getPath(), AppState.RUNNING, 1,
				desiredApp.getEnv(), appTmpRoute()));
		return desiredRelatedApps;
	}
}

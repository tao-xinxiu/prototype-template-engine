package com.orange.midstate.strategy;

import java.util.Set;

import com.orange.model.SiteDeploymentConfig;
import com.orange.model.state.AppState;
import com.orange.model.state.OverviewApp;

public class Canary extends Strategy {
	public Canary(Set<OverviewApp> updateApps, OverviewApp desiredApp, SiteDeploymentConfig config) {
		super(updateApps, desiredApp, config);
	}

	@Override
	public void onEnvUpdated() {
		updateApps.add(new OverviewApp(null, appTmpName(), desiredApp.getPath(), AppState.RUNNING, 1,
				desiredApp.getEnv(), appTmpRoute()));
	}
}

package com.orange.midstate.strategy;

import java.util.Set;

import com.orange.model.SiteDeploymentConfig;
import com.orange.model.state.AppState;
import com.orange.model.state.OverviewApp;

public class BlueGreen extends Strategy {
	public BlueGreen(Set<OverviewApp> updateApps, OverviewApp desiredApp, SiteDeploymentConfig config) {
		super(updateApps, desiredApp, config);
	}

	@Override
	public void onEnvUpdated() {
		updateApps.add(new OverviewApp(null, appTmpName(), desiredApp.getPath(), AppState.RUNNING,
				desiredApp.getInstances(), desiredApp.getEnv(), appTmpRoute()));
	}
}

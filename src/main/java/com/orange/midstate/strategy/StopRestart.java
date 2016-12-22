package com.orange.midstate.strategy;

import java.util.Set;
import java.util.stream.Collectors;

import com.orange.model.SiteDeploymentConfig;
import com.orange.model.state.AppState;
import com.orange.model.state.OverviewApp;

public class StopRestart extends Strategy {
	public StopRestart(Set<OverviewApp> updateApps, OverviewApp desiredApp, SiteDeploymentConfig config) {
		super(updateApps, desiredApp, config);
	}

	@Override
	public void onEnvUpdated() {
		AppState state = nameConflictedApp() == null ? AppState.RUNNING : AppState.CREATED;
		updateApps.add(new OverviewApp(null, appTmpName(), desiredApp.getPath(), state, desiredApp.getInstances(),
				desiredApp.getEnv(), appTmpRoute()));
	}

	@Override
	public void onStateUpdated() {
		Set<OverviewApp> oldRunningApps = oldApps().stream().filter(app -> app.getState() == AppState.RUNNING)
				.collect(Collectors.toSet());
		for (OverviewApp app : oldRunningApps) {
			app.setState(AppState.STAGED);
		}
		if (oldRunningApps.isEmpty()) {
			newApp().setState(desiredApp.getState());
		}
	}
}

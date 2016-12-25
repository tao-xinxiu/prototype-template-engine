package com.orange.midstate.strategy;

import java.util.Set;

import com.orange.model.SiteDeploymentConfig;
import com.orange.model.state.AppState;
import com.orange.model.state.OverviewApp;

public class StopRestart extends AppUpdateStrategy {
	public StopRestart(Set<OverviewApp> updateApps, OverviewApp desiredApp, SiteDeploymentConfig config) {
		super(updateApps, desiredApp, config);
	}

	@Override
	public Set<OverviewApp> onEnvUpdated() {
		Set<OverviewApp> desiredRelatedApps = Util.deepCopy(currentRelatedApps);
		AppState state = nameConflictedApp(desiredRelatedApps) == null ? AppState.RUNNING : AppState.CREATED;
		desiredRelatedApps.add(new OverviewApp(null, newAppName(), desiredApp.getPath(), state,
				desiredApp.getInstances(), desiredApp.getEnv(), appTmpRoute()));
		return desiredRelatedApps;
	}

	@Override
	public Set<OverviewApp> onStateUpdated() {
		Set<OverviewApp> desiredRelatedApps = Util.deepCopy(currentRelatedApps);
		OverviewApp instantiatedDesiredApp = instantiatedDesiredApp(desiredRelatedApps);
		if (desiredApp.getState() == AppState.RUNNING) {
			Set<OverviewApp> oldRunningApps = Util
					.searchByState(Util.exludedApps(desiredRelatedApps, instantiatedDesiredApp), AppState.RUNNING);
			if (oldRunningApps.isEmpty()) {
				instantiatedDesiredApp.setState(desiredApp.getState());
			} else {
				oldRunningApps.stream().forEach(app -> app.setState(AppState.STAGED));
			}
		} else {
			instantiatedDesiredApp.setState(desiredApp.getState());
		}

		return desiredRelatedApps;
	}
}

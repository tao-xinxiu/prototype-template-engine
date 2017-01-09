package com.orange.midstate.strategy;

import java.util.Set;

import com.orange.model.SiteDeploymentConfig;
import com.orange.model.state.AppState;
import com.orange.model.state.OverviewApp;

public class BlueGreen extends AppUpdateStrategy {
	public BlueGreen(Set<OverviewApp> updateApps, OverviewApp desiredApp, SiteDeploymentConfig config) {
		super(updateApps, desiredApp, config);
	}

	@Override
	public Set<OverviewApp> onEnvUpdated() {
		Set<OverviewApp> desiredRelatedApps = Util.deepCopy(currentRelatedApps);
		OverviewApp nameConflictedApp = nameConflictedApp(desiredRelatedApps, newAppName());
		if (nameConflictedApp != null) {
			desiredRelatedApps.remove(nameConflictedApp);
		} else {
			desiredRelatedApps.add(new OverviewApp(null, newAppName(), desiredApp.getPath(), AppState.RUNNING,
					desiredApp.getInstances(), desiredApp.getEnv(), appTmpRoute()));
		}
		return desiredRelatedApps;
	}

	@Override
	public Set<OverviewApp> onPathUpdated() {
		Set<OverviewApp> desiredRelatedApps = Util.deepCopy(currentRelatedApps);
		OverviewApp nameConflictedApp = nameConflictedApp(desiredRelatedApps, newAppName());
		if (nameConflictedApp != null) {
			desiredRelatedApps.remove(nameConflictedApp);
		} else {
			desiredRelatedApps.add(new OverviewApp(null, newAppName(), desiredApp.getPath(), AppState.RUNNING,
					desiredApp.getInstances(), desiredApp.getEnv(), appTmpRoute()));
		}
		return desiredRelatedApps;
	}
}

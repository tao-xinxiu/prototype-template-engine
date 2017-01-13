package com.orange.midstate.strategy;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import com.orange.model.SiteDeploymentConfig;
import com.orange.model.state.OverviewApp;
import com.orange.model.state.Route;

public abstract class OneUpdatingAppStrategy extends Strategy {
	private String updatingAppName;
	protected String newAppName;

	public OneUpdatingAppStrategy(Set<OverviewApp> currentApps, OverviewApp desiredApp,
			SiteDeploymentConfig config) {
		super(currentApps, desiredApp, config,
				app -> desiredApp.getGuid() != null ? desiredApp.getGuid().equals(app.getGuid())
						: Arrays.asList(desiredApp.getName(),
								desiredApp.getName() + config.getTmpNameSuffix())
								.contains(app.getName()));
		updatingAppName = desiredApp.getName() + config.getTmpNameSuffix();
		newAppName = Util.searchByName(currentRelatedApps, desiredApp.getName()) == null ? desiredApp.getName()
				: updatingAppName;
	}

	@Override
	public Set<OverviewApp> onNoInstantiatedDesiredApp() {
		return onPathUpdated();
	}

	@Override
	public Set<OverviewApp> onNameUpdated() {
		Set<OverviewApp> desiredRelatedApps = Util.deepCopy(currentRelatedApps);
		OverviewApp nameConflictedApp = Util.searchByName(desiredRelatedApps, desiredApp.getName());
		if (nameConflictedApp != null) {
			desiredRelatedApps.remove(nameConflictedApp);
		} else {
			instantiatedDesiredApp(desiredRelatedApps).setName(desiredApp.getName());
		}
		return desiredRelatedApps;
	}

	@Override
	protected OverviewApp instantiatedDesiredApp(Set<OverviewApp> currentRelatedApps) {
		if (desiredApp.getName().endsWith(config.getTmpNameSuffix())) {
			throw new IllegalStateException(String.format("Not support desired app [%s] using [%s] suffix.",
					desiredApp.getName(), config.getTmpNameSuffix()));
		}
		if (desiredApp.getPath() != null) {
			return null;
		}
		if (desiredApp.getGuid() != null) {
			return Util.searchApp(currentRelatedApps, app -> app.getGuid().equals(desiredApp.getGuid()));
		}
		Set<OverviewApp> desiredEnvApps = Util.search(currentRelatedApps,
				app -> app.getEnv().equals(desiredApp.getEnv()));
		switch (desiredEnvApps.size()) {
		case 0:
			return null;
		case 1:
			return desiredEnvApps.iterator().next();
		default:
			return Util.searchApp(currentRelatedApps, app -> app.getName().equals(updatingAppName));
		}
	}

	// return temporary route if tmpRouteDomain specified,
	// otherwise return app's desired routes.
	protected Set<Route> appTmpRoute() {
		return config.getTmpRouteDomain() == null ? desiredApp.listRoutes()
				: Collections.singleton(new Route(desiredApp.getName() + config.getTmpRouteHostSuffix(),
						config.getTmpRouteDomain()));
	}

}

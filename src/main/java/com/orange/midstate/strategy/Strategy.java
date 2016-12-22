package com.orange.midstate.strategy;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.orange.model.AppProperty;
import com.orange.model.SiteDeploymentConfig;
import com.orange.model.state.OverviewApp;
import com.orange.model.state.Route;

public abstract class Strategy {
	private static List<AppProperty> updateOrder = Collections.unmodifiableList(Arrays.asList(AppProperty.ENV,
			AppProperty.STATE, AppProperty.ROUTES, AppProperty.INSTANCES, AppProperty.NAME));
	protected Set<OverviewApp> updateApps;
	protected OverviewApp desiredApp;
	protected SiteDeploymentConfig config;

	public Strategy(Set<OverviewApp> updateApps, OverviewApp desiredApp, SiteDeploymentConfig config) {
		this.updateApps = updateApps;
		this.desiredApp = desiredApp;
		this.config = config;
	}

	public static List<AppProperty> getUpdateOrder() {
		return updateOrder;
	}

//	protected static void setUpdateOrder(List<AppProperty> updateOrder) {
//		if (updateOrder.size() != 5 || !updateOrder.containsAll(Arrays.asList(AppProperty.values()))) {
//			throw new IllegalStateException("Invalid update order.");
//		}
//		Strategy.updateOrder = Collections.unmodifiableList(updateOrder);
//	}

	public abstract void onEnvUpdated();

	public void onStateUpdated() {
		newApp().setState(desiredApp.getState());
	}

	public void onRoutesUpdated() {
		newApp().setRoutes(desiredApp.getRoutes());
	}

	public void onInstancesUpdated() {
		newApp().setInstances(desiredApp.getInstances());
	}

	public void onNameUpdated() {
		OverviewApp nameConflictedApp = nameConflictedApp();
		if (nameConflictedApp != null) {
			updateApps.remove(nameConflictedApp);
		} else {
			newApp().setName(desiredApp.getName());
		}
	}

	public void nothingUpdated() {
		updateApps.remove(oldApps());
	}

	// return temporary route if tmpRouteDomain specified,
	// otherwise return app's desired routes.
	protected Set<Route> appTmpRoute() {
		return config.getTmpRouteDomain() == null ? desiredApp.listRoutes()
				: Collections.singleton(
						new Route(desiredApp.getName() + config.getTmpRouteHostSuffix(), config.getTmpRouteDomain()));
	}

	protected Set<OverviewApp> oldApps() {
		return updateApps.stream().filter(app->app!=newApp()).collect(Collectors.toSet());
	}

	// return the app in "currentApps" named "appName"
	protected OverviewApp nameConflictedApp() {
		return oldApps().stream().filter(app -> app.getName().equals(desiredApp.getName())).findAny().orElse(null);
	}

	public OverviewApp newApp() {
		return desiredApp.getGuid() != null
				? updateApps.stream().filter(app -> app.getGuid().equals(desiredApp.getGuid())).findAny().orElse(null)
				: updateApps.stream().filter(app -> app.getEnv().equals(desiredApp.getEnv())).findAny().orElse(null);
	}

	protected String appTmpName() {
		return nameConflictedApp() == null ? desiredApp.getName() : desiredApp.getName() + config.getTmpNameSuffix();
	}
}

package com.orange.midstate.strategy;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.orange.model.AppProperty;
import com.orange.model.SiteDeploymentConfig;
import com.orange.model.state.OverviewApp;
import com.orange.model.state.Route;

public abstract class AppUpdateStrategy {
	private static List<AppProperty> updateOrder = Collections.unmodifiableList(Arrays.asList(AppProperty.Path,
			AppProperty.Env, AppProperty.State, AppProperty.Routes, AppProperty.Instances, AppProperty.Name));
	protected Set<OverviewApp> currentRelatedApps;
	protected OverviewApp desiredApp;
	protected SiteDeploymentConfig config;

	public AppUpdateStrategy(Set<OverviewApp> currentApps, OverviewApp desiredApp, SiteDeploymentConfig config) {
		this.desiredApp = desiredApp;
		this.config = config;
		this.currentRelatedApps = currentApps.stream().filter(relatedAppsPredicate()).collect(Collectors.toSet());
	}

	public static List<AppProperty> getUpdateOrder() {
		return updateOrder;
	}

	protected static void setUpdateOrder(List<AppProperty> updateOrder) {
		if (updateOrder.size() != 6 || !updateOrder.containsAll(Arrays.asList(AppProperty.values()))) {
			throw new IllegalStateException("Invalid update order.");
		}
		AppUpdateStrategy.updateOrder = Collections.unmodifiableList(updateOrder);
	}

	public OverviewApp getInstantiatedDesiredApp() {
		return instantiatedDesiredApp(currentRelatedApps);
	}

	/**
	 * The predicate of which apps in the current state is related to the
	 * desired app. The default strategy decide related apps by guid or name(if
	 * desired app guid not specified)
	 * 
	 * @return
	 */
	protected Predicate<OverviewApp> relatedAppsPredicate() {
		return app -> desiredApp.getGuid() != null ? desiredApp.getGuid().equals(app.getGuid())
				: relatedAppName().contains(app.getName());
	}

	public Set<OverviewApp> onNoInstantiatedDesiredApp() {
		return onPathUpdated();
	}

	public abstract Set<OverviewApp> onPathUpdated();

	public abstract Set<OverviewApp> onEnvUpdated();

	public Set<OverviewApp> onStateUpdated() {
		Set<OverviewApp> desiredRelatedApps = Util.deepCopy(currentRelatedApps);
		instantiatedDesiredApp(desiredRelatedApps).setState(desiredApp.getState());
		return desiredRelatedApps;
	}

	public Set<OverviewApp> onRoutesUpdated() {
		Set<OverviewApp> desiredRelatedApps = Util.deepCopy(currentRelatedApps);
		instantiatedDesiredApp(desiredRelatedApps).setRoutes(desiredApp.getRoutes());
		return desiredRelatedApps;
	}

	public Set<OverviewApp> onInstancesUpdated() {
		Set<OverviewApp> desiredRelatedApps = Util.deepCopy(currentRelatedApps);
		instantiatedDesiredApp(desiredRelatedApps).setInstances(desiredApp.getInstances());
		return desiredRelatedApps;
	}

	public Set<OverviewApp> onNameUpdated() {
		Set<OverviewApp> desiredRelatedApps = Util.deepCopy(currentRelatedApps);
		OverviewApp nameConflictedApp = nameConflictedApp(desiredRelatedApps, desiredApp.getName());
		if (nameConflictedApp != null) {
			desiredRelatedApps.remove(nameConflictedApp);
		} else {
			instantiatedDesiredApp(desiredRelatedApps).setName(desiredApp.getName());
		}
		return desiredRelatedApps;
	}

	public Set<OverviewApp> nothingUpdated() {
		Set<OverviewApp> desiredRelatedApps = Util.deepCopy(currentRelatedApps);
		desiredRelatedApps.remove(Util.exludedApps(desiredRelatedApps, instantiatedDesiredApp(desiredRelatedApps)));
		return desiredRelatedApps;
	}

	/**
	 * get reference of the current app intended to updated to the desired app.
	 * 
	 * @return
	 */
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
			return Util.searchApp(currentRelatedApps, app -> app.getName().equals(tmpAppName()));
		}
	}

	protected OverviewApp nameConflictedApp(Set<OverviewApp> currentRelatedApps, String name) {
		return Util.searchByName(Util.exludedApps(currentRelatedApps, instantiatedDesiredApp(currentRelatedApps)),
				name);
	}

	protected String newAppName() {
		return nameConflictedApp(currentRelatedApps, desiredApp.getName()) == null ? desiredApp.getName()
				: tmpAppName();
	}

	protected String tmpAppName() {
		return desiredApp.getName() + config.getTmpNameSuffix();
	}

	protected Set<String> relatedAppName() {
		return new HashSet<>(Arrays.asList(desiredApp.getName(), tmpAppName()));
	}

	// return temporary route if tmpRouteDomain specified,
	// otherwise return app's desired routes.
	protected Set<Route> appTmpRoute() {
		return config.getTmpRouteDomain() == null ? desiredApp.listRoutes()
				: Collections.singleton(
						new Route(desiredApp.getName() + config.getTmpRouteHostSuffix(), config.getTmpRouteDomain()));
	}
}

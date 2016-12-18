package com.orange.state;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.orange.model.AppState;
import com.orange.model.DeploymentConfig;
import com.orange.model.Overview;
import com.orange.model.OverviewApp;
import com.orange.model.OverviewSite;
import com.orange.model.PaaSSite;
import com.orange.model.Route;
import com.orange.model.SiteDeploymentConfig;
import com.orange.model.Strategy;

public class MidStateCalculator {
	private Strategy strategy;
	private DeploymentConfig deploymentConfig;

	public MidStateCalculator(Strategy strategy, DeploymentConfig deploymentConfig) {
		this.strategy = strategy;
		this.deploymentConfig = deploymentConfig;
	}

	/**
	 * calculate next mid state to achieve the final state, based on current
	 * state and different deployment configurations and update strategies.
	 * 
	 * @param currentState
	 * @param finalState
	 * @param strategy
	 * @param deploymentConfig
	 * @return
	 */
	public Overview calcMidStates(final Overview currentState, final Overview finalState) {
		// return null when arrived final state
		if (currentState.isInstantiation(finalState)) {
			return null;
		}
		Overview midState = new Overview();
		for (PaaSSite site : finalState.listPaaSSites()) {
			OverviewSite siteMidState = new OverviewSite();
			SiteDeploymentConfig config = deploymentConfig.getSiteDeploymentConfig(site.getName());
			for (OverviewApp desiredApp : finalState.getOverviewSite(site.getName()).getOverviewApps()) {
				String appTmpName = appTmpName(config, desiredApp);
				List<String> appNames = Arrays.asList(desiredApp.getName(), appTmpName);
				Set<OverviewApp> updateApps = new Overview(currentState).getOverviewSite(site.getName())
						.getOverviewApps().stream().filter(app -> appNames.contains(app.getName()))
						.collect(Collectors.toSet());
				OverviewApp newApp = updateApps.stream().filter(app -> app.getEnv().equals(desiredApp.getEnv()))
						.findAny().orElse(null);
				if (newApp == null) {
					boolean initDeploy = (oldApp(updateApps, desiredApp.getName()) == null);
					updateApps.add(newApp(config, desiredApp, initDeploy));
				} else if (!newApp.getState().equals(desiredApp.getState())) {
					changeAppState(updateApps, newApp, desiredApp);
				} else if (!newApp.getRoutes().equals(desiredApp.getRoutes())) {
					newApp.setRoutes(desiredApp.getRoutes());
				} else if (!(newApp.getInstances() == desiredApp.getInstances())) {
					newApp.setInstances(desiredApp.getInstances());
				} else if (!newApp.getName().equals(desiredApp.getName())) {
					OverviewApp oldApp = oldApp(updateApps, desiredApp.getName());
					if (oldApp != null) {
						updateApps.remove(oldApp);
					} else {
						newApp.setName(desiredApp.getName());
					}
				}
				siteMidState.addOverviewApps(updateApps);
			}
			midState.addPaaSSite(site, siteMidState);
		}
		return midState;
	}

	private OverviewApp newApp(SiteDeploymentConfig config, OverviewApp desiredApp, boolean initDeploy) {
		String appTmpName = initDeploy ? desiredApp.getName() : appTmpName(config, desiredApp);
		// new app connect to temporary route if tmpRouteDomain specified,
		// otherwise connect to its desired routes.
		List<Route> appTmpRoute = config.getTmpRouteDomain() == null ? desiredApp.listRoutes()
				: singleRoute(desiredApp.getName() + config.getTmpRouteHostSuffix(), config.getTmpRouteDomain());
		AppState state = AppState.RUNNING;
		int instances = desiredApp.getInstances();
		switch (strategy) {
		case STOPRESTART:
			if (!initDeploy) {
				state = AppState.CREATED;
			}
			break;
		case CANARY:
			instances = 1;
		case BLUEGREEN:
			break;
		}
		return new OverviewApp(null, appTmpName, desiredApp.getPath(), state, instances, desiredApp.getEnv(),
				appTmpRoute);
	}

	private void changeAppState(Set<OverviewApp> currentApps, OverviewApp currentApp, final OverviewApp desiredApp) {
		if (strategy == Strategy.STOPRESTART && !currentApp.getName().equals(desiredApp.getName())) {
			OverviewApp oldApp = oldApp(currentApps, desiredApp.getName());
			if (oldApp == null) {
				throw new IllegalStateException("Can't find the old version app. Unexpected situation during update.");
			}
			if (oldApp.getState() == AppState.RUNNING) {
				oldApp.setState(AppState.STAGED);
			} else {
				currentApp.setState(desiredApp.getState());
			}
		} else {
			currentApp.setState(desiredApp.getState());
		}
	}

	// return the app in "currentApps" named "appName"
	private OverviewApp oldApp(Set<OverviewApp> currentApps, String appName) {
		return currentApps.stream().filter(app -> app.getName().equals(appName)).findAny().orElse(null);
	}

	private String appTmpName(SiteDeploymentConfig config, OverviewApp desiredApp) {
		return desiredApp.getName() + config.getTmpNameSuffix();
	}

	private List<Route> singleRoute(String host, String domain) {
		return Collections.singletonList(new Route(host, domain));
	}
}

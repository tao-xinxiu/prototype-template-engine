package com.orange.midstate;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.orange.midstate.strategy.AppUpdateStrategy;
import com.orange.model.DeploymentConfig;
import com.orange.model.PaaSSite;
import com.orange.model.SiteDeploymentConfig;
import com.orange.model.state.Overview;
import com.orange.model.state.OverviewApp;
import com.orange.model.state.OverviewSite;

public class MidStateCalculator {
	private String strategyClass;
	private DeploymentConfig deploymentConfig;

	public MidStateCalculator(String strategyClass, DeploymentConfig deploymentConfig) {
		this.strategyClass = strategyClass;
		this.deploymentConfig = deploymentConfig;
	}

	/**
	 * calculate next mid state to achieve the final state, based on current
	 * state and different deployment configurations and update strategies.
	 * 
	 * @param currentState
	 * @param finalState
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
				List<String> appNames = Arrays.asList(desiredApp.getName(),
						desiredApp.getName() + config.getTmpNameSuffix());
				// updateApps is a deep copy of apps in current state which is
				// related (by name) to the desired app
				// TODO this injection may leave some current apps not related
				// to any desired app
				Set<OverviewApp> updateApps = new Overview(currentState).getOverviewSite(site.getName())
						.getOverviewApps().stream().filter(app -> appNames.contains(app.getName()))
						.collect(Collectors.toSet());
				try {
					AppUpdateStrategy strategy = (AppUpdateStrategy) Class.forName(strategyClass)
							.getConstructor(Set.class, OverviewApp.class, SiteDeploymentConfig.class)
							.newInstance(updateApps, desiredApp, config);
					OverviewApp newApp = strategy.newApp();
					if (newApp == null) {
						strategy.onEnvUpdated();
					} else if (!newApp.getState().equals(desiredApp.getState())) {
						strategy.onStateUpdated();
					} else if (!newApp.getRoutes().equals(desiredApp.getRoutes())) {
						strategy.onRoutesUpdated();
					} else if (!(newApp.getInstances() == desiredApp.getInstances())) {
						strategy.onInstancesUpdated();
					} else if (!newApp.getName().equals(desiredApp.getName())) {
						strategy.onNameUpdated();
					} else {
						strategy.nothingUpdated();
					}
					siteMidState.addOverviewApps(updateApps);
				} catch (InstantiationException | IllegalAccessException | ClassNotFoundException
						| IllegalArgumentException | InvocationTargetException | NoSuchMethodException
						| SecurityException e) {
					throw new IllegalStateException(String.format("Exception [%s] in Strategy [%s] instantiation.",
							e.getClass().getName(), strategyClass), e);
				}
			}
			midState.addPaaSSite(site, siteMidState);
		}
		return midState;
	}
}

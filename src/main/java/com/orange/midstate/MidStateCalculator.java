package com.orange.midstate;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

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
			Set<OverviewApp> currentApps = currentState.getOverviewSite(site.getName()).getOverviewApps();
			Set<OverviewApp> notRelatedApps = new HashSet<>(currentApps);
			for (OverviewApp desiredApp : finalState.getOverviewSite(site.getName()).getOverviewApps()) {
				try {
					AppUpdateStrategy strategy = (AppUpdateStrategy) Class.forName(strategyClass)
							.getConstructor(Set.class, OverviewApp.class, SiteDeploymentConfig.class)
							.newInstance(currentApps, desiredApp, config);
					Set<OverviewApp> currentRelatedApps = strategy.getCurrentRelatedApps();
					notRelatedApps.removeAll(currentRelatedApps);
					OverviewApp instantiatedDesiredApp = strategy.instantiatedDesiredApp(currentRelatedApps);
					if (instantiatedDesiredApp == null) {
						siteMidState.addOverviewApps(strategy.onEnvUpdated());
					} else if (!instantiatedDesiredApp.getState().equals(desiredApp.getState())) {
						siteMidState.addOverviewApps(strategy.onStateUpdated());
					} else if (!instantiatedDesiredApp.getRoutes().equals(desiredApp.getRoutes())) {
						siteMidState.addOverviewApps(strategy.onRoutesUpdated());
					} else if (!(instantiatedDesiredApp.getInstances() == desiredApp.getInstances())) {
						siteMidState.addOverviewApps(strategy.onInstancesUpdated());
					} else if (!instantiatedDesiredApp.getName().equals(desiredApp.getName())) {
						siteMidState.addOverviewApps(strategy.onNameUpdated());
					} else {
						siteMidState.addOverviewApps(strategy.nothingUpdated());
					}
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

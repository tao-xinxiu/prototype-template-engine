package com.orange.midstate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import com.orange.midstate.strategy.AppUpdateStrategy;
import com.orange.model.AppProperty;
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
	@SuppressWarnings("unchecked")
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
			for (OverviewApp desiredApp : finalState.getOverviewSite(site.getName()).getOverviewApps()) {
				try {
					AppUpdateStrategy strategy = (AppUpdateStrategy) Class.forName(strategyClass)
							.getConstructor(Set.class, OverviewApp.class, SiteDeploymentConfig.class)
							.newInstance(currentApps, desiredApp, config);
					OverviewApp instantiatedDesiredApp = strategy.getInstantiatedDesiredApp();
					if (instantiatedDesiredApp == null) {
						siteMidState.addOverviewApps(strategy.onEnvUpdated());
						continue;
					}
					List<AppProperty> updateOrder = AppUpdateStrategy.getUpdateOrder();
					for (AppProperty property : updateOrder) {
						if (propertyChanged(instantiatedDesiredApp, desiredApp, property)) {
							siteMidState.addOverviewApps((Set<OverviewApp>) strategy.getClass()
									.getMethod(String.format("on%sUpdated", property)).invoke(strategy));
							break;
						}
						if (property == updateOrder.get(updateOrder.size() - 1)) {
							siteMidState.addOverviewApps(strategy.nothingUpdated());
						}
					}
				} catch (InstantiationException | IllegalAccessException | ClassNotFoundException
						| IllegalArgumentException | InvocationTargetException | NoSuchMethodException
						| SecurityException e) {
					throw new IllegalStateException(
							String.format("Exception [%s] in Strategy [%s] instantiation or method calling.",
									e.getClass().getName(), strategyClass),
							e);
				}
			}
			midState.addPaaSSite(site, siteMidState);
		}
		return midState;
	}

	private boolean propertyChanged(OverviewApp currentApp, OverviewApp desiredApp, AppProperty property) {
		try {
			Method getPropertyMethod = OverviewApp.class.getMethod("get" + property);
			return !getPropertyMethod.invoke(currentApp).equals(getPropertyMethod.invoke(desiredApp));
		} catch (NoSuchMethodException | SecurityException e) {
			throw new IllegalStateException(String.format("Exception [%s] in getting [%s].[%s] method.",
					e.getClass().getName(), OverviewApp.class, "get" + property), e);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new IllegalStateException(String.format("Exception [%s] in calling [%s].[%s] method.",
					e.getClass().getName(), OverviewApp.class, "get" + property), e);
		}
	}
}

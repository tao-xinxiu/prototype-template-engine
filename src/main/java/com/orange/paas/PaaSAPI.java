package com.orange.paas;

import java.util.List;

import com.orange.model.Application;
import com.orange.model.PaaSTarget;

public abstract class PaaSAPI {
	protected PaaSTarget target;
	protected RouteFactory routeFactory;

	public PaaSAPI(PaaSTarget target, RouteFactory routeFactory) {
		this.target = target;
		this.routeFactory = routeFactory;
	}

	public String getTargetName() {
		return target.getName();
	}

	/**
	 * create an app and return its id, if an app with specific name existed,
	 * return the id of the app directly
	 * 
	 * @param appProperty
	 * @return
	 */
	public abstract String createAppIfNotExist(Application appProperty);

	public abstract void prepareApp(String appId, Application appProperty);

	public abstract void startAppAndWaitUntilRunning(String appId);

	public abstract void stopApp(String appId);

	public abstract void deleteApp(String appId);

	public abstract void updateApp(String appId, Application appProperty);

	public String getRouteId(String hostname, String domainKey) {
		return routeFactory.getRouteId(hostname, domainKey);
	}

	public String createRouteIfNotExist(String hostname, String domainKey) {
		return routeFactory.createRouteIfNotExist(hostname, domainKey);
	}

	public void createRouteMapping(String appId, String routeId) {
		routeFactory.createRouteMapping(appId, routeId);
	}

	public void deleteRouteMapping(String appId, String routeId) {
		if (routeId == null || appId == null) {
			return;
		}
		routeFactory.deleteRouteMapping(appId, routeId);
	}

	public abstract List<String> listSpaceAppsId();

	public abstract String getAppId(String appName);

	public abstract String getAppName(String appId);

	public abstract Object getAppEnv(String appId, String envKey);

	public String getAppVersion(String appId) {
		return (String) getAppEnv(appId, "APP_VERSION");
	}
}

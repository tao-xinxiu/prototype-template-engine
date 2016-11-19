package com.orange.paas;

import java.util.List;

import com.orange.model.*;

public abstract class PaaSAPI {
	protected PaaSSite site;
	protected RouteFactory routeFactory;

	public PaaSAPI(PaaSSite site, RouteFactory routeFactory) {
		this.site = site;
		this.routeFactory = routeFactory;
	}

	public String getSiteName() {
		return site.getName();
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
		routeFactory.deleteRouteMapping(appId, routeId);
	}
	
	public List<String> listAppRoutes(String appId) {
		return routeFactory.listAppRoutes(appId);
	}

	public abstract List<String> listSpaceAppsId();

	public abstract String getAppId(String appName);

	public abstract String getAppName(String appId);

	public abstract Object getDropletEnv(String dropletId, String envKey);
	
	public String getDropletVersion(String dropletId) {
		return (String) getDropletEnv(dropletId, "APP_VERSION");
	}
	
	public abstract List<String> listAppDropletsId(String appId);
	
	public abstract DropletState getAppDropletState(String appId, String dropletId);
	
	public abstract OverviewSite getOverviewSite();
}

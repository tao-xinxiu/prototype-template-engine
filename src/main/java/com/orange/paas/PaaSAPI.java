package com.orange.paas;

import java.util.List;
import java.util.Map;

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

	public Map<String, String> getDomains() {
		return routeFactory.domains;
	}

	public abstract String createAppIfNotExist(OverviewApp app);

	public abstract void startAppAndWaitUntilRunning(String appId);

	public abstract void stopApp(String appId);

	public abstract void deleteApp(String appId);

	public abstract void updateApp(OverviewApp app, Map<String, String> env);

	public abstract void updateApp(OverviewApp app);

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

	public List<Route> listAppRoutes(String appId) {
		return routeFactory.listAppRoutes(appId);
	}

	public abstract List<String> listSpaceAppsId();

	public abstract String getAppId(String appName);

	public abstract String getAppName(String appId);

	public abstract Map<String, String> listDropletEnv(String appId, String dropletId);

	public String getDropletVersion(String appId, String dropletId) {
		return (String) listDropletEnv(appId, dropletId).get("APP_VERSION");
	}

	public abstract List<String> listAppDropletsId(String appId);

	public abstract DropletState getAppDropletState(String appId, String dropletId);

	public abstract OverviewSite getOverviewSite();

	public abstract String createAppWithOneDroplet(OverviewApp app);

	// upload the package and stage the droplet
	public abstract String prepareDroplet(String appId, OverviewDroplet droplet);

	public abstract void deleteDroplet(String dropletId);

	public abstract void assignDroplet(String appId, String dropletId);

	public void mapAppRoutes(String appId, List<Route> routes) {
		routeFactory.mapAppRoutes(appId, routes);
	}

	public void unmapAppRoutes(String appId, List<Route> routes) {
		routeFactory.unmapAppRoutes(appId, routes);
	}
}

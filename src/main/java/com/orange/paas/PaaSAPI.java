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

	public abstract OverviewSite getOverviewSite();

	public abstract String createAppIfNotExist(OverviewApp app);

	public abstract void startAppAndWaitUntilRunning(String appId);

	public abstract void stopApp(String appId);

	public abstract void deleteApp(String appId);

	public abstract void updateAppName(String appId, String name);

	public abstract void updateAppEnv(String appId, Map<String, String> env);

	public abstract void scaleApp(String appId, int instances);

	// upload the package and stage the droplet
	public abstract void prepareApp(OverviewApp app);

	public abstract void deleteDroplet(String dropletId);

	public abstract void assignDroplet(String appId, String dropletId);

	public List<Route> listAppRoutes(String appId) {
		return routeFactory.listAppRoutes(appId);
	}

	public void mapAppRoutes(String appId, List<Route> routes) {
		routeFactory.mapAppRoutes(appId, routes);
	}

	public void unmapAppRoutes(String appId, List<Route> routes) {
		routeFactory.unmapAppRoutes(appId, routes);
	}
}

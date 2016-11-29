package com.orange.paas;

import java.util.List;
import java.util.Map;

import com.orange.model.PaaSAccessInfo;
import com.orange.model.PaaSSite;
import com.orange.model.Route;

public abstract class RouteFactory {
	protected PaaSAccessInfo siteAccessInfo;
	protected Map<String, String> domains;

	public RouteFactory(PaaSSite site) {
		this.siteAccessInfo = site.getAccessInfo();
		this.domains = site.getDomains();
	}

	public abstract String getRouteId(String hostname, String domainKey);

	public abstract String createRouteIfNotExist(String hostname, String domainKey);

	public abstract void createRouteMapping(String appId, String routeId);

	public abstract void deleteRouteMapping(String appId, String routeId);

	public abstract List<Route> listAppRoutes(String appId);

	/**
	 * create routes if not exist, and map routes to the app
	 * 
	 * @param appId
	 * @param routes
	 */
	public abstract void mapAppRoutes(String appId, List<Route> routes);

	/**
	 * unmap routes to the app and delete routes
	 * 
	 * @param appId
	 * @param routes
	 */
	public abstract void unmapAppRoutes(String appId, List<Route> routes);
}

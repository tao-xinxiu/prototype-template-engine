package com.orange.paas;

import java.util.List;

import com.orange.model.PaaSAccessInfo;
import com.orange.model.PaaSSite;
import com.orange.model.Route;

public abstract class RouteFactory {
	protected PaaSAccessInfo siteAccessInfo;

	public RouteFactory(PaaSSite site) {
		this.siteAccessInfo = site.getAccessInfo();
	}

	public abstract List<Route> listAppRoutes(String appId);

	/**
	 * create routes if not exist, and map routes to the app
	 * 
	 * @param appId
	 * @param routes
	 */
	public abstract void mapAppRoutes(String appId, List<Route> routes);

	/**
	 * unmap routes to the app
	 * 
	 * @param appId
	 * @param routes
	 */
	public abstract void unmapAppRoutes(String appId, List<Route> routes);
}

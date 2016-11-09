package com.orange.paas;

import java.util.Map;

import com.orange.model.PaaSAccessInfo;
import com.orange.model.PaaSSite;

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
}

package com.orange.paas;

import java.util.Map;

public abstract class RouteFactory {
	protected Map<String, String> domains;
	
	public RouteFactory(Map<String, String> domains) {
		this.domains = domains;
	}
	
	public abstract String getRouteId(String hostname, String domainKey);

	public abstract String createRouteIfNotExist(String hostname, String domainKey);

	public abstract void createRouteMapping(String appId, String routeId);

	public abstract void deleteRouteMapping(String appId, String routeId);

}

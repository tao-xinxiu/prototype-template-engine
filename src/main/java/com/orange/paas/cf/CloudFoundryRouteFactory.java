package com.orange.paas.cf;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.model.PaaSSite;
import com.orange.model.Route;
import com.orange.paas.RouteFactory;

public class CloudFoundryRouteFactory extends RouteFactory {
	private static final Logger logger = LoggerFactory.getLogger(CloudFoundryRouteFactory.class);
	private CloudFoundryOperations operations;

	public CloudFoundryOperations getOperations() {
		return operations;
	}

	public CloudFoundryRouteFactory(PaaSSite site) {
		super(site);
		this.operations = new CloudFoundryOperations(siteAccessInfo);
	}

	@Override
	public List<Route> listAppRoutes(String appId) {
		List<Route> appRoutes = new ArrayList<>();
		for (String routeId : operations.listMappedRoutesId(appId)) {
			appRoutes.add(operations.getRoute(routeId));
		}
		return appRoutes;
	}

	@Override
	public void mapAppRoutes(String appId, List<Route> routes) {
		for (Route route : routes) {
			String domainId = operations.getDomainId(route.getDomain());
			String routeId = operations.getRouteId(route.getHostname(), domainId);
			if (routeId == null) {
				routeId = operations.createRoute(route.getHostname(), domainId);
			}
			operations.createRouteMapping(appId, routeId);
			logger.info("route [{}] mapped to the app [{}]", routeId, appId);
		}
	}

	@Override
	public void unmapAppRoutes(String appId, List<Route> routes) {
		for (Route route : routes) {
			String domainId = operations.getDomainId(route.getDomain());
			String routeId = operations.getRouteId(route.getHostname(), domainId);
			if (routeId != null) {
				String routeMappingId = operations.getRouteMappingId(appId, routeId);
				if (routeMappingId != null) {
					operations.deleteRouteMapping(routeMappingId);
				}
			}
			logger.info("route [{}] unmapped from the app [{}]", routeId, appId);
		}
	}
}

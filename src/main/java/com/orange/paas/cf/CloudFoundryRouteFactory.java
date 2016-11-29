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

	private String getDomainId(String domainKey) {
		return operations.getDomainId(this.domains.get(domainKey));
	}

	@Override
	public String getRouteId(String hostname, String domainKey) {
		String routeId = operations.getRouteId(hostname, getDomainId(domainKey));
		if (routeId != null) {
			logger.info("[{}] route with hostname [{}] existed with id: [{}]", domainKey, hostname, routeId);
		} else {
			logger.info("[{}] route with hostname [{}] not existed", domainKey, hostname);
		}
		return routeId;
	}

	@Override
	public String createRouteIfNotExist(String hostname, String domainKey) {
		String routeId = getRouteId(hostname, domainKey);
		if (routeId == null) {
			routeId = operations.createRoute(hostname, getDomainId(domainKey));
			logger.info("[{}] route with hostname [{}] created with id: [{}]", domainKey, hostname, routeId);
		}
		return routeId;
	}

	@Override
	public void createRouteMapping(String appId, String routeId) {
		operations.createRouteMapping(appId, routeId);
		logger.info("route [{}] mapped to the app [{}]", routeId, appId);
	}

	@Override
	public void deleteRouteMapping(String appId, String routeId) {
		if (routeId == null || appId == null) {
			return;
		}
		String routeMappingId = operations.getRouteMappingId(appId, routeId);
		if (routeMappingId != null) {
			logger.info("route-mapping id found: [{}]", routeMappingId);
			operations.deleteRouteMapping(routeMappingId);
			logger.info("route [{}] unmapped with app [{}]", routeId, appId);
		} else {
			logger.info("route-mapping not found");
		}
	}

	@Override
	public List<Route> listAppRoutes(String appId) {
		List<Route> appRoutes = new ArrayList<>();
		for (String routeId : operations.listMappedRoutesId(appId)) {
			String host = operations.getRouteHost(routeId);
			String domain = operations.getDomainString(operations.getRouteDomainId(routeId));
			appRoutes.add(new Route(host, domain));
		}
		return appRoutes;
	}

	@Override
	public void mapAppRoutes(String appId, List<Route> routes) {
		for (Route route : routes) {
			if (!domains.containsValue(route.getDomain())) {
				throw new IllegalStateException(String.format(
						"The domain [%s] of the route [%s] is not contained in the site [%s] specification",
						route.getDomain(), route, siteAccessInfo.getName()));
			}
			String domainId = operations.getDomainId(route.getDomain());
			String routeId = operations.getRouteId(route.getHostname(), domainId);
			if (routeId == null) {
				operations.createRoute(route.getHostname(), domainId);
			}
			operations.createRouteMapping(appId, routeId);
			logger.info("route [{}] mapped to the app [{}]", routeId, appId);
		}
	}

	@Override
	public void unmapAppRoutes(String appId, List<Route> routes) {
		// TODO
	}

}

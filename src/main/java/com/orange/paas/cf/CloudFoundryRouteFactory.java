package com.orange.paas.cf;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.model.PaaSSite;
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
	public List<String> listAppRoutes(String appId) {
		List<String> appRoutes = new ArrayList<>();
		for (String routeId : operations.listMappedRoutesId(appId)) {
			String host = operations.getRouteHost(routeId);
			String domain = operations.getDomainString(operations.getRouteDomainId(routeId));
			appRoutes.add(String.format("%s.%s", host, domain));
		}
		return appRoutes;
	}

	@Override
	public void mapAppRoutes(String appId, List<String> routes) {
		for (String route : routes) {
			String[] routeSplit = route.split("\\.", 2);
			if (routeSplit.length != 2) {
				throw new IllegalStateException(String.format("route [%s] format error", route));
			}
			String hostname = routeSplit[0];
			String domain = routeSplit[1];
			if (!domains.containsValue(domain)) {
				throw new IllegalStateException(String.format(
						"The domain [%s] of the route [%s] is not contained in the site [%s] specification", domain, route, siteAccessInfo.getName()));
			}
			String domainId = operations.getDomainId(domain);
			String routeId = operations.getRouteId(hostname, domainId);
			if (routeId == null) {
				operations.createRoute(hostname, domainId);
			}
			operations.createRouteMapping(appId, routeId);
			logger.info("route [{}] mapped to the app [{}]", routeId, appId);
		}
	}
}

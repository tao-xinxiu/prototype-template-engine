package com.orange.paas.cf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.model.PaaSTarget;
import com.orange.paas.RouteFactory;

public class CloudFoundryRouteFactory extends RouteFactory {
	private CloudFoundryOperations operations;
	private static final Logger logger = LoggerFactory.getLogger(CloudFoundryRouteFactory.class);

	public CloudFoundryRouteFactory(PaaSTarget target) {
		super(target.getDomains());
		this.operations = new CloudFoundryOperations(target);
	}
	
	private String getDomainId(String domainKey) {
		logger.error("domain {}", domains);
		logger.error("local domain: {}; global domain: {}", this.domains.get("local"), this.domains.get("global")); 
		return operations.getDomainId(this.domains.get(domainKey));
	}
	
	@Override
	public String getRouteId(String hostname, String domainKey) {
		String routeId = operations.getRouteId(getDomainId(domainKey), hostname);
		logger.info("{} route id {} found for hostname {}", domainKey, routeId, hostname);
		return routeId;
	}

	@Override
	public String createRouteIfNotExist(String hostname, String domainKey) {
		String routeId = getRouteId(hostname, domainKey);
		if (routeId != null) {
			logger.info("{} route with hostname {} existed with id: {}", domainKey, hostname, routeId);
			return routeId;
		}
		routeId = operations.createRoute(hostname, getDomainId(domainKey));
		logger.info("{} route with hostname {} created with id: {}", domainKey, hostname, routeId);
		return routeId;
	}

	@Override
	public void createRouteMapping(String appId, String routeId) {
		operations.createRouteMapping(appId, routeId);
		logger.info("route {} mapped to the app {}", routeId, appId);
	}

	@Override
	public void deleteRouteMapping(String appId, String routeId) {
		String routeMappingId = operations.getRouteMappingId(appId, routeId);
		if (routeMappingId != null) {
			logger.info("route-mapping id found: {}", routeMappingId);
			operations.deleteRouteMapping(routeMappingId);
			logger.info("route {} unmapped with app {}", routeId, appId);
		} else {
			logger.info("route-mapping not found");
		}
	}

}
